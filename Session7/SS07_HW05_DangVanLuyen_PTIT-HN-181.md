# BÀI 5: SÁNG TẠO - Thiết kế hệ thống RAG CRM Ticket Assistant hoàn chỉnh

## 1. Sơ đồ luồng xử lý dữ liệu ASCII

```text
┌──────────────────────────────┐
│ Nhân viên CSKH               │
│ Gửi khiếu nại mới            │
└──────────────┬───────────────┘
               │
               │ POST /api/v1/tickets/rag
               ▼
┌──────────────────────────────┐
│ REST Controller              │
│ TicketRagController          │
└──────────────┬───────────────┘
               │
               │ newComplaint
               ▼
┌──────────────────────────────┐
│ TicketRagService             │
│ Xử lý nghiệp vụ RAG          │
└──────────────┬───────────────┘
               │
               │ 1. Embedding
               ▼
┌──────────────────────────────┐
│ Embedding Model              │
│ Chuyển complaint → vector    │
└──────────────┬───────────────┘
               │
               │ 2. Similarity Search
               ▼
┌──────────────────────────────┐
│ PostgreSQL + pgvector        │
│ vector_store                 │
│ Top 3, threshold >= 0.6      │
└──────────────┬───────────────┘
               │
               │ Retrieved Tickets
               ▼
        ┌─────────────────┐
        │ Có ticket phù   │
        │ hợp >= 0.6 ?    │
        └────────┬────────┘
             ┌───┴───┐
            NO       YES
             │         │
             ▼         ▼
┌──────────────────┐  ┌──────────────────────┐
│ KHÔNG gọi LLM    │  │ Xây dựng Grounding   │
│                  │  │ Context từ Top 3     │
│ Trả về thông báo │  └──────────┬───────────┘
│ chuyển chuyên gia│             │
└────────┬─────────┘             ▼
         │             ┌──────────────────────┐
         │             │ ChatModel / LLM      │
         │             │ Soạn Draft Response  │
         │             └──────────┬───────────┘
         │                        │
         └────────────┬───────────┘
                      ▼
           ┌────────────────────────┐
           │ ChatbotResponse        │
           │                        │
           │ - draftEmail           │
           │ - references (Top 3)   │
           └────────────┬───────────┘
                        │
                        ▼
              ┌──────────────────┐
              │ Client / CSKH    │
              └──────────────────┘
```

---

# 2. Giải pháp thiết kế phòng thủ dữ liệu

Hệ thống áp dụng nguyên tắc:

> **Không có Context đáng tin cậy → Không gọi LLM.**

Ngưỡng similarity được đặt là:

```text
similarityThreshold = 0.6
```

Số lượng kết quả tối đa:

```text
topK = 3
```

Luồng kiểm tra:

```text
Customer Complaint
       ↓
Similarity Search
       ↓
Có kết quả similarity >= 0.6?
       │
   ┌───┴───┐
   │       │
  NO      YES
   │       │
   ▼       ▼
Không     Gọi LLM
gọi LLM      │
   │         ▼
   │      Draft Response
   │         +
   │      References
   │
   ▼
Fallback Response
```

### Trường hợp không tìm thấy ticket phù hợp

Nếu PostgreSQL không trả về ticket nào có similarity >= `0.6`, Service **không gọi ChatModel**.

Thay vào đó:

```text
Xin lỗi khách hàng vì chưa thể đưa ra hướng xử lý tự động.
Hệ thống sẽ chuyển tiếp khiếu nại tới chuyên viên cao cấp
để được kiểm tra và xử lý thủ công.
```

`references` được trả về danh sách rỗng.

Điều này giúp ngăn chặn trường hợp LLM tự sử dụng kiến thức chung để "đoán" chính sách của doanh nghiệp.

---

# 3. Java Record TicketDto

```java
package com.rikkei.retail.crm.dto;

public record TicketDto(
        String ticketId,
        String complaint,
        String resolution,
        Double similarity
) {
}
```

`TicketDto` chứa thông tin cần thiết của ticket lịch sử để trả về cho Client:

- `ticketId`: Mã ticket cũ.
- `complaint`: Nội dung khiếu nại.
- `resolution`: Cách xử lý đã được áp dụng.
- `similarity`: Độ tương đồng với khiếu nại mới.

---

# 4. Java Record ChatbotResponse

```java
package com.rikkei.retail.crm.dto;

import java.util.List;

public record ChatbotResponse(
        String draftEmail,
        List<TicketDto> references
) {
}
```

Record này là response cuối cùng trả về Client.

Nó gồm:

- `draftEmail`: Nội dung thư phản hồi gợi ý.
- `references`: Danh sách tối đa 3 ticket lịch sử được sử dụng làm Context.

---

# 5. TicketRagService

```java
package com.rikkei.retail.crm.service;

import com.rikkei.retail.crm.dto.ChatbotResponse;
import com.rikkei.retail.crm.dto.TicketDto;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Service
public class TicketRagService {

    private static final double SIMILARITY_THRESHOLD = 0.6;
    private static final int TOP_K = 3;

    private final VectorStore vectorStore;
    private final ChatModel chatModel;

    public TicketRagService(
            VectorStore vectorStore,
            ChatModel chatModel
    ) {
        this.vectorStore = vectorStore;
        this.chatModel = chatModel;
    }

    @Transactional(readOnly = true)
    public ChatbotResponse processComplaint(String newComplaint) {

        // 1. Tìm kiếm các ticket tương đồng trong Vector Store
        SearchRequest searchRequest = SearchRequest.builder()
                .query(newComplaint)
                .topK(TOP_K)
                .similarityThreshold(SIMILARITY_THRESHOLD)
                .build();

        List<Document> documents = vectorStore.similaritySearch(searchRequest);

        // 2. Defensive Validation
        // Nếu không có ticket phù hợp thì tuyệt đối không gọi LLM.
        if (documents == null || documents.isEmpty()) {

            String fallbackMessage = """
                    Xin lỗi quý khách, hiện tại chúng tôi chưa thể đưa ra
                    hướng xử lý tự động cho khiếu nại này.

                    Hệ thống sẽ chuyển tiếp yêu cầu tới chuyên viên cao cấp
                    để kiểm tra và xử lý thủ công.

                    Nhân viên chăm sóc khách hàng sẽ liên hệ lại với quý khách
                    sau khi có kết quả kiểm tra.
                    """;

            return new ChatbotResponse(
                    fallbackMessage,
                    Collections.emptyList()
            );
        }

        // 3. Chuyển các Document thành Context cho LLM
        String context = buildContext(documents);

        // 4. Xây dựng Grounding Prompt
        String prompt = """
                Bạn là chuyên gia chăm sóc khách hàng chuyên nghiệp.

                NHIỆM VỤ:
                Dựa CHỈ trên các ticket lịch sử được cung cấp bên dưới,
                hãy soạn một thư phản hồi gợi ý cho khiếu nại mới.

                KHIẾU NẠI MỚI:
                %s

                CONTEXT - TICKET LỊCH SỬ:
                %s

                QUY TẮC:
                1. Chỉ sử dụng thông tin có trong CONTEXT.
                2. Không được tự tạo chính sách hoàn tiền, voucher,
                   đổi sản phẩm hoặc bồi thường nếu CONTEXT không đề cập.
                3. Không được tự suy diễn thông tin nghiệp vụ.
                4. Không được đưa ra cam kết ngoài CONTEXT.
                5. Thư phải lịch sự, chuyên nghiệp và đồng cảm.
                6. Nếu Context không đủ thông tin để xử lý thì phải
                   yêu cầu nhân viên chuyển ticket cho chuyên viên cao cấp.
                
                Chỉ trả về nội dung thư phản hồi, không giải thích thêm.
                """.formatted(newComplaint, context);

        // 5. Chỉ tại đây mới gọi LLM
        String draftEmail = chatModel.call(prompt);

        // 6. Tạo references trả về Client
        List<TicketDto> references = documents.stream()
                .map(this::toTicketDto)
                .toList();

        return new ChatbotResponse(
                draftEmail,
                references
        );
    }

    private String buildContext(List<Document> documents) {

        StringBuilder context = new StringBuilder();

        for (int i = 0; i < documents.size(); i++) {

            Document document = documents.get(i);

            context.append("=== TICKET ")
                    .append(i + 1)
                    .append(" ===\n");

            context.append(document.getText())
                    .append("\n\n");

            if (document.getMetadata() != null) {
                context.append("Metadata: ")
                        .append(document.getMetadata())
                        .append("\n\n");
            }
        }

        return context.toString();
    }

    private TicketDto toTicketDto(Document document) {

        var metadata = document.getMetadata();

        String ticketId = String.valueOf(
                metadata.getOrDefault("ticketId", "UNKNOWN")
        );

        String complaint = String.valueOf(
                metadata.getOrDefault("complaint", document.getText())
        );

        String resolution = String.valueOf(
                metadata.getOrDefault("resolution", "")
        );

        Double similarity = null;

        Object score = metadata.get("distance");

        if (score instanceof Number number) {
            similarity = number.doubleValue();
        }

        return new TicketDto(
                ticketId,
                complaint,
                resolution,
                similarity
        );
    }
}
```

---

# 6. REST Controller

```java
package com.rikkei.retail.crm.controller;

import com.rikkei.retail.crm.dto.ChatbotResponse;
import com.rikkei.retail.crm.service.TicketRagService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/tickets")
public class TicketRagController {

    private final TicketRagService ticketRagService;

    public TicketRagController(TicketRagService ticketRagService) {
        this.ticketRagService = ticketRagService;
    }

    @PostMapping("/rag")
    public ResponseEntity<ChatbotResponse> generateDraft(
            @RequestBody String complaint
    ) {

        ChatbotResponse response =
                ticketRagService.processComplaint(complaint);

        return ResponseEntity.ok(response);
    }
}
```

---

# 7. Ví dụ Request

Client gửi:

```http
POST /api/v1/tickets/rag
Content-Type: text/plain
```

Body:

```text
Đơn hàng APP-9981 của tôi giao chậm 4 ngày.
Nhân viên trước đó cam kết giao trong 24 giờ.
Tôi muốn được kiểm tra và xử lý ngay.
```

---

# 8. Ví dụ Response khi tìm thấy ticket tương đồng

```json
{
  "draftEmail": "Kính gửi Quý khách,\n\nChúng tôi rất tiếc về sự chậm trễ của đơn hàng...",
  "references": [
    {
      "ticketId": "TICKET-1001",
      "complaint": "Khách hàng phản ánh đơn hàng giao chậm...",
      "resolution": "Kiểm tra trạng thái vận chuyển và cập nhật cho khách hàng.",
      "similarity": 0.89
    },
    {
      "ticketId": "TICKET-2045",
      "complaint": "Đơn hàng giao trễ so với thời gian cam kết...",
      "resolution": "Chuyển bộ phận vận chuyển kiểm tra và phản hồi.",
      "similarity": 0.81
    },
    {
      "ticketId": "TICKET-3021",
      "complaint": "Khách hàng chưa nhận được hàng theo thời gian dự kiến...",
      "resolution": "Kiểm tra tình trạng giao hàng và liên hệ khách hàng.",
      "similarity": 0.74
    }
  ]
}
```

---

# 9. Ví dụ Response khi không tìm thấy Context phù hợp

Nếu tất cả kết quả đều có similarity < `0.6`, `VectorStore` không trả về ticket phù hợp.

Service không gọi LLM và trả về:

```json
{
  "draftEmail": "Xin lỗi quý khách, hiện tại chúng tôi chưa thể đưa ra hướng xử lý tự động cho khiếu nại này.\n\nHệ thống sẽ chuyển tiếp yêu cầu tới chuyên viên cao cấp để kiểm tra và xử lý thủ công.\n\nNhân viên chăm sóc khách hàng sẽ liên hệ lại với quý khách sau khi có kết quả kiểm tra.",
  "references": []
}
```

Đây là cơ chế **Defensive Validation** quan trọng nhất của hệ thống.

---

# 10. Giải trình chi tiết luồng xử lý

## Bước 1: Client gửi khiếu nại

Nhân viên CSKH gửi khiếu nại mới tới:

```text
POST /api/v1/tickets/rag
```

Controller nhận nội dung complaint và chuyển cho `TicketRagService`.

## Bước 2: Embedding Model

Khi gọi:

```java
vectorStore.similaritySearch(searchRequest);
```

Spring AI VectorStore sẽ sử dụng EmbeddingModel được cấu hình để chuyển khiếu nại thành vector.

Ví dụ:

```text
Complaint
   ↓
Embedding Model
   ↓
[0.12, -0.45, 0.82, ...]
```

## Bước 3: Similarity Search

Service tạo:

```java
SearchRequest searchRequest = SearchRequest.builder()
        .query(newComplaint)
        .topK(3)
        .similarityThreshold(0.6)
        .build();
```

Ý nghĩa:

- `topK(3)`: tối đa lấy 3 ticket.
- `similarityThreshold(0.6)`: chỉ lấy ticket có độ tương đồng đạt ngưỡng yêu cầu.

Database PostgreSQL + pgvector thực hiện tìm kiếm vector tương đồng trong `vector_store`.

## Bước 4: Defensive Validation

Service kiểm tra:

```java
if (documents == null || documents.isEmpty()) {
    ...
}
```

Nếu không có kết quả:

```text
Không có Context
      ↓
KHÔNG gọi LLM
      ↓
Fallback Response
      ↓
references = []
```

Đây là cơ chế chống Hallucination ở tầng Application, không chỉ phụ thuộc vào Prompt.

## Bước 5: Xây dựng Context

Nếu có ticket phù hợp, Service lấy nội dung các ticket và ghép thành Context:

```text
=== TICKET 1 ===
...

=== TICKET 2 ===
...

=== TICKET 3 ===
...
```

Context này được đưa vào Prompt.

## Bước 6: Grounding Prompt

Prompt yêu cầu LLM:

- Chỉ sử dụng Context.
- Không tự tạo chính sách.
- Không tự hứa hoàn tiền.
- Không tự tạo voucher.
- Không tự tạo quy trình bảo hành.
- Không đưa ra cam kết ngoài Context.

Nhờ đó, LLM được **ground** vào dữ liệu lịch sử đã được retrieval.

## Bước 7: Gọi ChatModel

Chỉ khi có Context đạt ngưỡng similarity:

```java
String draftEmail = chatModel.call(prompt);
```

LLM tạo Draft Response.

## Bước 8: Trả về kết quả

Service kết hợp:

```text
draftEmail
+
references
```

và trả về:

```java
ChatbotResponse
```

cho REST Controller.

---

# 11. Tối ưu hóa Transaction

Service sử dụng:

```java
@Transactional(readOnly = true)
```

Mục đích là xác định transaction này chỉ phục vụ việc đọc dữ liệu.

Lợi ích:

- Thể hiện rõ nghiệp vụ không thực hiện ghi dữ liệu.
- Giảm overhead của transaction trong trường hợp database hỗ trợ tối ưu read-only.
- Tránh vô tình thực hiện các thao tác ghi trong transaction này.
- Phù hợp với nghiệp vụ Retrieval của RAG.

Tuy nhiên, cần lưu ý rằng việc gọi LLM là một thao tác I/O bên ngoài database.

Trong hệ thống Production, không nên giữ database transaction mở trong suốt thời gian chờ LLM nếu transaction đó không thực sự cần thiết.

Một kiến trúc tối ưu hơn có thể tách:

```text
Transaction 1
    ↓
Vector Search
    ↓
Transaction kết thúc
    ↓
Build Context
    ↓
Call LLM
    ↓
Response
```

Điều này tránh giữ connection/database transaction trong thời gian LLM phản hồi.

---

# 12. Tổng kết kiến trúc

Kiến trúc hoàn chỉnh:

```text
                    ┌───────────────────┐
                    │    CSKH Client    │
                    └─────────┬─────────┘
                              │
                              ▼
                    ┌───────────────────┐
                    │ REST Controller   │
                    └─────────┬─────────┘
                              │
                              ▼
                    ┌───────────────────┐
                    │ TicketRagService  │
                    └─────────┬─────────┘
                              │
                              ▼
                    ┌───────────────────┐
                    │ Embedding Model   │
                    └─────────┬─────────┘
                              │
                              ▼
                 ┌──────────────────────────┐
                 │ PostgreSQL + pgvector    │
                 │ vector_store              │
                 │ Top 3 / threshold >= 0.6 │
                 └────────────┬─────────────┘
                              │
                    ┌─────────▼─────────┐
                    │ Defensive         │
                    │ Validation        │
                    └───────┬───────────┘
                            │
                  ┌─────────┴─────────┐
                  │                   │
             Không có >= 0.6      Có >= 0.6
                  │                   │
                  ▼                   ▼
          ┌──────────────┐    ┌───────────────┐
          │ Fallback     │    │ Grounding     │
          │ Không gọi AI │    │ Prompt        │
          └──────┬───────┘    └───────┬───────┘
                 │                    │
                 │                    ▼
                 │             ┌───────────────┐
                 │             │ ChatModel LLM │
                 │             └───────┬───────┘
                 │                    │
                 └──────────┬─────────┘
                            ▼
                  ┌─────────────────────┐
                  │ ChatbotResponse     │
                  │                     │
                  │ draftEmail          │
                  │ references          │
                  └─────────────────────┘
```

## Kết luận

Giải pháp RAG trên đáp ứng đầy đủ các yêu cầu:

- Sử dụng **PostgreSQL + pgvector** làm Vector Store.
- Sử dụng **Embedding Model** để vector hóa khiếu nại mới.
- Tìm kiếm tối đa **3 ticket tương đồng**.
- Áp dụng `similarityThreshold = 0.6`.
- Có **Defensive Validation** trước khi gọi LLM.
- Nếu không có Context đạt ngưỡng thì **tuyệt đối không gọi LLM**.
- Sử dụng **Grounding Prompt** để giảm Hallucination.
- Trả về `ChatbotResponse` gồm `draftEmail` và `references`.
- Sử dụng `@Transactional(readOnly = true)` cho phần truy vấn dữ liệu.
- Tách biệt rõ ràng Controller và Service theo kiến trúc Spring Boot.

**Nguyên tắc quan trọng nhất của hệ thống:**

> **Retrieval không đủ tin cậy → Không gọi LLM → Chuyển xử lý cho con người.**

Đây là cơ chế phòng thủ giúp CRM Ticket Assistant ưu tiên **tính chính xác và an toàn nghiệp vụ** thay vì cố gắng tạo ra câu trả lời trong mọi trường hợp.