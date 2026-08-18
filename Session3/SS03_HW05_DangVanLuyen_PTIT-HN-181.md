# 1. Ý tưởng thiết kế Self-Healing Extraction

Luồng xử lý:

```text
                 Raw Text
                    |
                    v
             +-------------+
             |    Prompt   |
             +-------------+
                    |
                    v
                  LLM
                    |
                    v
              JSON Response
                    |
                    v
          BeanOutputConverter
                    |
             +------+------+
             |             |
             v             v
        Parse OK       Parse Error
             |             |
             v             v
        Return DTO    Catch Exception
                           |
                           v
                 Lấy exception.getMessage()
                           |
                           v
                 Feedback vào Prompt
                           |
                           v
                         Retry
                           |
             +-------------+-------------+
             |                           |
          Success                    Max Retry
             |                           |
             v                           v
        Return DTO              Default Record
```

---

# 2. Java Record mẫu

Ví dụ hệ thống HR trích xuất ứng viên:

```java
package com.rikkei.hr.dto;

import java.util.List;

public record CandidateExtraction(

        String fullName,

        String email,

        String phone,

        List<String> skills,

        Integer yearsExperience

) {

}
```

---

# 3. Default Record fallback

Khi AI không sửa được JSON:

```java
private CandidateExtraction defaultCandidate() {

    return new CandidateExtraction(
            "UNKNOWN",
            null,
            null,
            List.of(),
            0
    );
}
```

---

# 4. SelfHealingExtractionService

```java
package com.rikkei.hr.service;


import com.rikkei.hr.dto.CandidateExtraction;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.converter.BeanOutputConverter;

import org.springframework.stereotype.Service;


import java.util.List;



@Service
public class SelfHealingExtractionService {


    private final ChatModel chatModel;


    private final BeanOutputConverter<CandidateExtraction> converter;



    public SelfHealingExtractionService(
            ChatModel chatModel
    ) {

        this.chatModel = chatModel;

        this.converter =
                new BeanOutputConverter<>(
                        CandidateExtraction.class
                );
    }



    public CandidateExtraction extractWithRetry(
            String rawText,
            int maxRetries
    ) {


        String feedback = "";



        for(int attempt = 1;
            attempt <= maxRetries;
            attempt++) {


            try {


                String prompt = buildPrompt(
                        rawText,
                        feedback
                );



                String response =
                        chatModel.call(
                                new Prompt(prompt)
                        )
                        .getResult()
                        .getOutput()
                        .getText();



                /*
                 * Nếu JSON hợp lệ
                 * convert thành công
                 */

                return converter.convert(response);



            } catch (Exception exception) {



                /*
                 * Lấy lỗi Jackson
                 * gửi lại cho AI
                 */

                feedback = """

                Lần trước bạn trả về JSON không hợp lệ.

                Lỗi parser:

                %s


                Hãy sửa lại JSON.
                Chỉ trả về JSON hợp lệ.
                Không thêm markdown.
                Không giải thích.

                """
                .formatted(
                        exception.getMessage()
                );



                System.out.println(
                        "Extraction failed attempt "
                        + attempt
                        + ": "
                        + exception.getMessage()
                );

            }

        }



        /*
         * Vượt quá số lần retry
         */

        return defaultCandidate();

    }





    private String buildPrompt(
            String rawText,
            String feedback
    ) {


        return """

        Bạn là AI JSON Extraction Engine.

        Nhiệm vụ:
        Trích xuất dữ liệu ứng viên từ text.

        Input:

        %s


        Yêu cầu:

        - Chỉ trả JSON.
        - Không markdown.
        - Không giải thích.

        %s


        JSON Schema:

        %s

        """
        .formatted(
                rawText,
                feedback,
                converter.getFormatInstructions()
        );

    }





    private CandidateExtraction defaultCandidate() {


        return new CandidateExtraction(
                "UNKNOWN",
                null,
                null,
                List.of(),
                0
        );

    }

}
```

---

# 5. Ví dụ hoạt động Error Feedback Loop

## Lần gọi đầu tiên

AI trả:

```text
 id="6s8c4v"
{
 "fullName": "Nguyen Van A",
 "email": "a@gmail.com",
 "skills": ["Java"]
```

Thiếu:

```text
}
```

BeanOutputConverter gọi Jackson:

```
JsonProcessingException

Unexpected end-of-input:
expected close marker for Object
```

---

## Service bắt lỗi

Tạo feedback:

```text
 id="x6r1u8"
Lần trước bạn trả về JSON không hợp lệ.

Lỗi parser:

Unexpected end-of-input:
expected close marker for Object

Hãy sửa lại JSON.
Chỉ trả về JSON hợp lệ.
```

---

## Lần gọi thứ 2

AI sửa:

```json
{
 "fullName":"Nguyen Van A",
 "email":"a@gmail.com",
 "phone":"0988888888",
 "skills":["Java"],
 "yearsExperience":2
}
```

Parse thành công.

---

# 6. Phân tích Trade-off của Self-Healing

## Ưu điểm

## 1. Tăng độ tin cậy hệ thống

Không bị:

```
LLM Error
   |
   v
Exception
   |
   v
HTTP 500
```

Thay bằng:

```
LLM Error
   |
   v
Feedback
   |
   v
Repair
   |
   v
Success
```

Đặc biệt hữu ích khi:

* Model local nhỏ.
* Qwen 7B.
* Llama nhỏ.
* Prompt phức tạp.

---

## 2. Giảm lỗi thủ công

Không cần viết quá nhiều logic:

````java
if(json.contains("```"))
if(missingBracket)
if(fieldMissing)
````

AI tự xử lý một phần lỗi format.

---

## 3. Có thể mở rộng

Có thể bổ sung feedback:

* Validation error.
* Missing field.
* Regex email sai.
* Enum không hợp lệ.

Ví dụ:

```text
Email không đúng format.
Hãy sửa field email.
```

---

# Nhược điểm

---

# 1. Tăng chi phí token

Mỗi lần retry tạo thêm:

* Prompt input.
* Error message.
* Schema.
* Context.

Ví dụ:

Lần đầu:

```
Input token:
1500
Output:
300
```

Retry:

```
Input:
1500
+
500 error feedback

Output:
300
```

Tổng:

```
~4100 tokens
```

Nếu dùng model cloud sẽ tăng chi phí.

---

# 2. Tăng latency

Không retry:

```
Request
 |
LLM 5s
 |
Response
```

Self-healing:

```
Request

 |
LLM 5s

 |
Parse Error

 |
Retry

 |
LLM 5s

 |
Response
```

Latency:

```
5s
+
5s

= 10s
```

Nếu:

```java
maxRetries = 3
```

có thể:

```
15-20 giây
```

---

# 3. Không đảm bảo sửa được lỗi logic

Self-healing tốt với:

* Thiếu dấu `}`.
* Sai JSON format.
* Sai quote.

Nhưng kém với:

Ví dụ:

AI trả:

```json
{
 "yearsExperience": 50
}
```

JSON hợp lệ nhưng dữ liệu sai.

Cần thêm:

```text
Validation Layer
```

---

# 4. Có nguy cơ vòng lặp tốn tài nguyên

Nếu:

```java
maxRetries = 100
```

sẽ gây:

* Tốn token.
* Chậm hệ thống.
* Quá tải API.

Nên giới hạn:

```java
maxRetries <= 3
```

hoặc:

```java
maxRetries = 2
```

---

# 7. Kiến trúc Production nên dùng

Không nên chỉ dựa vào Self-Healing:

```text
LLM
 |
 v
JSON Parser
 |
 v
Validation
 |
 +---- Fail
 |
 v
Error Feedback Retry
 |
 v
Final Output
```

Kết hợp:

* Temperature thấp (`0 - 0.2`).
* BeanOutputConverter.
* JSON mode nếu model hỗ trợ.
* Validation nghiệp vụ.
* Retry giới hạn.

Self-Healing là lớp **khôi phục lỗi**, không thay thế cho validation và thiết kế prompt tốt.
