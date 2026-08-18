# 1. Kiến trúc tổng quan Module ETL Resume Parser

Luồng xử lý:

```text
                 CV Resume Text
                       |
                       |
                       v
              +----------------+
              |    EXTRACT     |
              | Nhận CV thô    |
              +----------------+
                       |
                       |
                       v
              +----------------+
              |   TRANSFORM    |
              |                |
              | ChatModel      |
              | BeanOutput     |
              | Converter      |
              +----------------+
                       |
                       |
                       v
        CandidateExtraction (Java Record)

                       |
                       |
                       v

              +----------------+
              |    VALIDATE    |
              |
              | - Name != null |
              | - Email regex  |
              | - Experience   |
              |   >= 0         |
              +----------------+
                       |
                       |
                       v

              +----------------+
              |      LOAD      |
              |
              | Candidate JPA  |
              | Repository     |
              +----------------+
                       |
                       |
                       v

              +----------------+
              | SQL Database   |
              | Candidate      |
              | Table          |
              +----------------+
```

---

# 2. CandidateExtraction Record

Record dùng làm DTO trung gian giữa LLM và hệ thống.

```java
package com.rikkei.hr.dto;

import java.util.List;

public record CandidateExtraction(

        String fullName,

        String phone,

        String email,

        List<String> skills,

        Integer yearsExperience

) {
}
```

---

# 3. Candidate Entity

```java
package com.rikkei.hr.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.List;


@Entity
@Table(name = "candidates")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Candidate {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @Column(nullable = false)
    private String fullName;


    private String phone;


    @Column(unique = true)
    private String email;


    @ElementCollection
    @CollectionTable(
            name = "candidate_skills",
            joinColumns = @JoinColumn(name = "candidate_id")
    )
    @Column(name = "skill")
    private List<String> skills;


    private Integer yearsExperience;
}
```

---

# 4. CandidateRepository

```java
package com.rikkei.hr.repository;


import com.rikkei.hr.entity.Candidate;
import org.springframework.data.jpa.repository.JpaRepository;


public interface CandidateRepository
        extends JpaRepository<Candidate, Long> {


}
```

---

# 5. Prompt dùng cho Resume Parser

Ví dụ prompt gửi LLM:

```text
Bạn là AI Resume Parser của hệ thống HR.

Nhiệm vụ:
Trích xuất thông tin ứng viên từ CV dạng text.

Chỉ lấy dữ liệu xuất hiện trong CV.
Không tự tạo thông tin.

Các trường cần lấy:

- fullName
- phone
- email
- skills
- yearsExperience


CV:

{resumeText}


Yêu cầu output:
Chỉ trả về JSON hợp lệ.

Không markdown.
Không giải thích.
Không thêm text ngoài JSON.

{formatInstructions}
```

---

# 6. CandidateETLService

Luồng:

1. Nhận CV.
2. Gọi LLM.
3. Convert JSON → Record.
4. Validate.
5. Save DB.

```java
package com.rikkei.hr.service;


import com.rikkei.hr.dto.CandidateExtraction;
import com.rikkei.hr.entity.Candidate;
import com.rikkei.hr.repository.CandidateRepository;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.converter.BeanOutputConverter;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.util.regex.Pattern;



@Service
public class CandidateETLService {


    private final ChatModel chatModel;

    private final CandidateRepository candidateRepository;


    public CandidateETLService(
            ChatModel chatModel,
            CandidateRepository candidateRepository
    ) {

        this.chatModel = chatModel;
        this.candidateRepository = candidateRepository;
    }



    public Candidate processResume(String resumeText) {


        /*
         * TRANSFORM
         * Gọi LLM
         */

        BeanOutputConverter<CandidateExtraction> converter =
                new BeanOutputConverter<>(CandidateExtraction.class);


        String promptText = """
                Bạn là AI Resume Parser.

                Trích xuất dữ liệu CV:

                %s


                %s
                """
                .formatted(
                        resumeText,
                        converter.getFormatInstructions()
                );


        String response =
                chatModel.call(
                        new Prompt(promptText)
                )
                .getResult()
                .getOutput()
                .getText();



        CandidateExtraction extraction =
                converter.convert(response);



        /*
         * VALIDATE
         */

        validateCandidate(extraction);



        /*
         * LOAD
         */

        Candidate candidate =
                new Candidate();


        candidate.setFullName(
                extraction.fullName()
        );

        candidate.setPhone(
                extraction.phone()
        );

        candidate.setEmail(
                extraction.email()
        );

        candidate.setSkills(
                extraction.skills()
        );

        candidate.setYearsExperience(
                extraction.yearsExperience()
        );


        return candidateRepository.save(candidate);
    }



    private void validateCandidate(
            CandidateExtraction candidate
    ) {


        // Validation 1:
        // kiểm tra họ tên

        if(candidate.fullName() == null
                || candidate.fullName().isBlank()) {

            throw new IllegalArgumentException(
                    "Candidate name is required"
            );
        }



        // Validation 2:
        // kiểm tra email

        if(candidate.email() == null
                ||
                !Pattern.matches(
                    "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$",
                    candidate.email()
                )
        ){

            throw new IllegalArgumentException(
                    "Invalid email format"
            );
        }



        // Validation 3:
        // kinh nghiệm

        if(candidate.yearsExperience() == null
                ||
                candidate.yearsExperience() < 0
        ){

            throw new IllegalArgumentException(
                    "Invalid experience"
            );
        }

    }
}
```

---

# 7. Phân tích Trade-off @Transactional với LLM Call

## Trường hợp 1: Gọi LLM bên trong @Transactional

Ví dụ:

```java
@Transactional
public Candidate processResume(String text){

    String json =
        chatModel.call(prompt);

    return repository.save(candidate);
}
```

## Ưu điểm

### 1. Atomic transaction

Nếu:

* LLM thành công.
* Save database lỗi.

Toàn bộ transaction rollback.

Ví dụ:

```
LLM Success
     |
     |
Save DB Failed
     |
     |
Rollback
```

Dữ liệu không bị lưu nửa chừng.

---

## Nhược điểm lớn

### 1. Giữ connection database quá lâu

Luồng:

```
BEGIN TRANSACTION

       |
       |
       v

Database Connection được giữ

       |
       |
       v

Gọi LLM API

(5-30 giây)

       |
       |
       v

Save DB

COMMIT
```

Trong thời gian chờ LLM:

* Connection vẫn bị chiếm.
* Connection pool giảm.
* Nhiều request đồng thời gây nghẽn.

Ví dụ:

Pool:

```
maximumPoolSize = 10
```

10 request gọi AI cùng lúc:

```
10 connection bị giữ 20 giây
```

Request thứ 11:

```
Timeout waiting for connection
```

---

# Trường hợp 2: Gọi LLM bên ngoài Transaction

Thiết kế tốt hơn:

```java
public Candidate processResume(String text){


    CandidateExtraction data =
          callLLM(text);


    validate(data);


    return saveCandidate(data);

}
```

và:

```java
@Transactional
public Candidate saveCandidate(
        CandidateExtraction data
){

    return repository.save(candidate);

}
```

---

## Ưu điểm

### 1. Không giữ connection khi gọi AI

Luồng:

```
Call LLM

(20 giây)

     |
     |
     v

Mở transaction

     |
     |
Save DB

     |
     |
Commit
```

Database chỉ bị giữ trong vài ms.

---

### 2. Tăng khả năng mở rộng

Có thể xử lý nhiều CV:

```
100 CV

AI Processing Pool

        |

Database Transaction ngắn
```

---

## Nhược điểm

### 1. Không rollback được LLM

Ví dụ:

```
LLM trả JSON

        |

Save DB lỗi
```

Không thể rollback việc gọi AI.

Tuy nhiên LLM call không phải database operation nên rollback thường không có ý nghĩa.

---

# Kết luận kiến trúc nên dùng

Production nên thiết kế:

```
Controller

   |
   |
CandidateETLService

   |
   +------------+
   |            |
   v            v

LLM Call     Validate

(no transaction)

                |
                |
                v

        @Transactional

        Save Database
```

Lý do:

* API LLM là network operation chậm.
* Database transaction phải ngắn.
* Tránh giữ connection pool.
* Dễ retry LLM khi lỗi.

---

# 8. Log chạy thực tế

## Prompt gửi LLM

```text
SYSTEM:

Bạn là AI Resume Parser.

Extract candidate information.

CV:

Nguyễn Văn An

Email:
nguyen.an@gmail.com

Phone:
0987654321

Kinh nghiệm:
3 năm Java Backend Developer.

Skills:
Java, Spring Boot, MySQL


Return JSON only.

```

---

## AI Response

```json
{
  "fullName": "Nguyễn Văn An",
  "phone": "0987654321",
  "email": "nguyen.an@gmail.com",
  "skills": [
    "Java",
    "Spring Boot",
    "MySQL"
  ],
  "yearsExperience": 3
}
```

---

## Sau BeanOutputConverter

```text
CandidateExtraction(
 fullName=Nguyễn Văn An,
 phone=0987654321,
 email=nguyen.an@gmail.com,
 skills=[Java, Spring Boot, MySQL],
 yearsExperience=3
)
```

---

## Database sau khi Load

Bảng `candidates`:

| id | full_name     | email                                             | years_experience |
| -- | ------------- | ------------------------------------------------- | ---------------- |
| 1  | Nguyễn Văn An | [nguyen.an@gmail.com](mailto:nguyen.an@gmail.com) | 3                |

Module ETL hoàn tất:

```
CV Text
  |
Extract
  |
LLM Transform
  |
Validation
  |
JPA Save
  |
SQL Database
```
