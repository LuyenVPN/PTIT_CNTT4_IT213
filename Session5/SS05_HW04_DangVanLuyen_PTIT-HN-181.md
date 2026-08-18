# BÀI 4: Tích hợp - Thiết kế ChatMemory bền vững cho Booking Agent

## 1. Giải pháp thiết kế và phân tách phiên chat

### 1.1. Vấn đề của InMemoryChatMemory

Trong môi trường Local, `InMemoryChatMemory` có thể đáp ứng tốt nhu cầu thử nghiệm vì lịch sử hội thoại được lưu trực tiếp trong RAM của ứng dụng.

Tuy nhiên, khi triển khai Production với nhiều Kubernetes Pods:

```text
                         Load Balancer
                              |
              +---------------+---------------+
              |               |               |
           Pod A           Pod B           Pod C
              |               |               |
         RAM Memory       RAM Memory       RAM Memory
```

Mỗi Pod có một vùng nhớ riêng.

Ví dụ:

```text
Request 1 → Pod A → lưu conversation vào RAM của Pod A

Request 2 → Pod B → không tìm thấy conversation cũ
```

Kết quả là AI có thể mất context giữa các request.

Ngoài ra, khi Pod restart:

```text
Pod A RAM
    ↓
Restart
    ↓
Memory bị xóa
    ↓
Lịch sử hội thoại bị mất
```

Do đó, `InMemoryChatMemory` không phù hợp cho hệ thống Production cần scale-out.

---

## 2. Giải pháp Persistent Chat Memory

Thay thế:

```text
InMemoryChatMemory
```

bằng:

```text
JdbcChatMemory
```

và lưu lịch sử hội thoại tập trung trong MySQL.

Kiến trúc:

```text
                         Client
                           |
                           | conversationId
                           v
                     Load Balancer
                           |
             +-------------+-------------+
             |             |             |
           Pod A         Pod B         Pod C
             |             |             |
             +-------------+-------------+
                           |
                     JdbcChatMemory
                           |
                       JdbcTemplate
                           |
                           v
                         MySQL
                    Chat Memory Table
```

Tất cả các Pod đều truy cập cùng một database.

Vì vậy, request của cùng một `conversationId` có thể được xử lý bởi bất kỳ Pod nào mà vẫn truy xuất được lịch sử hội thoại.

---

# 3. Cơ chế phân tách Session bằng conversationId

Mỗi cuộc hội thoại phải có một ID duy nhất.

Ví dụ:

```text
conversationId =
550e8400-e29b-41d4-a716-446655440000
```

Lịch sử được phân biệt dựa trên ID này.

Ví dụ:

```text
Conversation A
ID = abc-123
    ↓
Message 1
Message 2
Message 3

Conversation B
ID = xyz-789
    ↓
Message 1
Message 2
```

AI Agent chỉ lấy lịch sử thuộc `conversationId` tương ứng.

Nhờ vậy, lịch sử của khách hàng A không bị trộn với khách hàng B.

---

# 4. Mã nguồn Java cấu hình JdbcChatMemory

## 4.1. DatabaseChatMemoryConfig

```java
package com.rhotels.config;

import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.jdbc.JdbcChatMemory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class DatabaseChatMemoryConfig {

    @Bean
    public ChatMemory chatMemory(JdbcTemplate jdbcTemplate) {
        return new JdbcChatMemory(jdbcTemplate);
    }
}
```

`JdbcTemplate` được Spring Boot tự động cấu hình dựa trên thông tin datasource trong `application.properties` hoặc `application.yml`.

Ví dụ:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/rhotels
spring.datasource.username=root
spring.datasource.password=${DB_PASSWORD}
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
```

---

# 5. Cấu hình ChatClient với MessageChatMemoryAdvisor

ChatClient cần được kết nối với `JdbcChatMemory` thông qua `MessageChatMemoryAdvisor`.

```java
package com.rhotels.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ChatClientConfig {

    @Bean
    public ChatClient chatClient(
            ChatClient.Builder builder,
            ChatMemory chatMemory
    ) {

        return builder
                .defaultSystem("""
                        Bạn là AI Booking Agent của R-Hotels.
                        Hãy hỗ trợ khách hàng tìm kiếm và đặt phòng khách sạn.
                        Luôn sử dụng thông tin từ lịch sử hội thoại khi phù hợp.
                        """)
                .defaultAdvisors(
                        MessageChatMemoryAdvisor.builder(chatMemory)
                                .build()
                )
                .build();
    }
}
```

Ở đây:

```text
ChatClient
    ↓
MessageChatMemoryAdvisor
    ↓
JdbcChatMemory
    ↓
JdbcTemplate
    ↓
MySQL
```

`MessageChatMemoryAdvisor` chịu trách nhiệm kết nối lịch sử hội thoại vào quá trình gọi ChatClient.

---

# 6. REST Controller nhận conversationId

Controller phải nhận `conversationId` từ Client.

Nếu Client không gửi `conversationId`, hệ thống sẽ tự động tạo UUID mới.

```java
package com.rhotels.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/booking")
public class BookingController {

    private final ChatClient chatClient;

    public BookingController(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @GetMapping("/chat")
    public String chat(
            @RequestParam String message,
            @RequestParam(required = false) String conversationId
    ) {

        // Tạo Session ID mới nếu Client chưa cung cấp
        if (conversationId == null || conversationId.isBlank()) {
            conversationId = UUID.randomUUID().toString();
        }

        return chatClient
                .prompt()
                .user(message)
                .advisors(advisor -> advisor
                        .param(
                                ChatMemory.CONVERSATION_ID,
                                conversationId
                        )
                )
                .call()
                .content();
    }
}
```

Điểm quan trọng nhất là:

```java
ChatMemory.CONVERSATION_ID
```

được gán bằng:

```java
conversationId
```

Thông qua:

```java
.advisors(advisor -> advisor
        .param(
        ChatMemory.CONVERSATION_ID,
        conversationId
        )
)
```

Spring AI sử dụng giá trị này để xác định lịch sử hội thoại nào cần được đọc và ghi.

---

# 7. Luồng xử lý request đầu tiên

Client gửi:

```http
GET /api/booking/chat?message=Tôi muốn đặt phòng Deluxe
```

Không có `conversationId`.

Controller thực hiện:

```java
conversationId = UUID.randomUUID().toString();
```

Ví dụ:

```text
550e8400-e29b-41d4-a716-446655440000
```

Sau đó truyền vào ChatClient:

```text
chat_memory_conversation_id
        =
550e8400-e29b-41d4-a716-446655440000
```

Lịch sử được lưu vào MySQL.

Response nên trả lại `conversationId` cho Client để Client sử dụng trong các request tiếp theo.

Trong thực tế, nên trả về một DTO thay vì chỉ trả về `String`:

```java
public record ChatResponse(
        String conversationId,
        String message
) {
}
```

Controller:

```java
@GetMapping("/chat")
public ChatResponse chat(
        @RequestParam String message,
        @RequestParam(required = false) String conversationId
) {

    if (conversationId == null || conversationId.isBlank()) {
        conversationId = UUID.randomUUID().toString();
    }

    String response = chatClient
            .prompt()
            .user(message)
            .advisors(advisor -> advisor
                    .param(
                            ChatMemory.CONVERSATION_ID,
                            conversationId
                    )
            )
            .call()
            .content();

    return new ChatResponse(
            conversationId,
            response
    );
}
```

Như vậy Client nhận được:

```json
{
  "conversationId": "550e8400-e29b-41d4-a716-446655440000",
  "message": "Tôi có thể giúp bạn tìm phòng Deluxe..."
}
```

Client phải lưu `conversationId` và gửi lại ở các request tiếp theo.

---

# 8. Ví dụ hội thoại

## Request 1

Client:

```text
message = "Tôi muốn đặt phòng Deluxe"
conversationId = null
```

Server tạo:

```text
conversationId = abc-123
```

MySQL lưu:

```text
abc-123 → User: Tôi muốn đặt phòng Deluxe
abc-123 → AI: Bạn muốn đặt phòng vào ngày nào?
```

---

## Request 2

Client gửi:

```text
message = "Ngày mai"
conversationId = abc-123
```

Request có thể được xử lý bởi Pod B.

Pod B truy vấn:

```text
conversationId = abc-123
```

MySQL trả về lịch sử:

```text
User: Tôi muốn đặt phòng Deluxe
AI: Bạn muốn đặt phòng vào ngày nào?
User: Ngày mai
```

AI hiểu `"Ngày mai"` đang liên quan đến yêu cầu đặt phòng Deluxe trước đó.

---

# 9. Cơ chế đồng bộ dữ liệu khi Scale-out

Điểm quan trọng nhất của kiến trúc này là **database được dùng làm nguồn lưu trữ trung tâm**.

Không còn:

```text
Pod A → RAM A
Pod B → RAM B
Pod C → RAM C
```

mà chuyển thành:

```text
Pod A ─┐
Pod B ─┼──→ MySQL
Pod C ─┘
```

Ví dụ:

```text
Request 1
    ↓
Load Balancer
    ↓
Pod A
    ↓
JdbcChatMemory
    ↓
MySQL
    ↓
conversationId = abc-123
```

Request tiếp theo:

```text
Request 2
    ↓
Load Balancer
    ↓
Pod C
    ↓
JdbcChatMemory
    ↓
MySQL
    ↓
conversationId = abc-123
    ↓
Lấy được lịch sử cũ
```

Không cần cấu hình **sticky session** vì session không còn phụ thuộc vào RAM của một Pod cụ thể.

---

# 10. Khả năng chịu lỗi khi Pod Restart

Giả sử Pod A bị restart:

```text
Pod A
  ↓
Restart
  ↓
RAM bị xóa
```

Nhưng lịch sử hội thoại nằm trong MySQL:

```text
MySQL
  ↓
conversationId = abc-123
  ↓
History vẫn tồn tại
```

Khi request tiếp theo được Load Balancer chuyển sang Pod B:

```text
Pod B
  ↓
JdbcChatMemory
  ↓
MySQL
  ↓
Lấy conversationId = abc-123
```

AI vẫn có thể tiếp tục cuộc hội thoại.

---

# 11. Lợi ích của Persistent Chat Memory

| Tiêu chí          | InMemoryChatMemory | JdbcChatMemory            |
| ----------------- | ------------------ | ------------------------- |
| Lưu trữ           | RAM                | MySQL                     |
| Multi-Pod         | ❌ Không phù hợp    | ✅ Phù hợp                 |
| Load Balancer     | Dễ mất context     | ✅ Không phụ thuộc Pod     |
| Restart           | ❌ Mất dữ liệu      | ✅ Dữ liệu vẫn tồn tại     |
| Scale-out         | ❌ Hạn chế          | ✅ Tốt                     |
| Persistence       | ❌ Không            | ✅ Có                      |
| Session isolation | Phụ thuộc JVM      | Dựa trên `conversationId` |

---

# 12. Kết luận

Giải pháp thay thế `InMemoryChatMemory` bằng `JdbcChatMemory` giúp R-Hotels xây dựng hệ thống AI Booking Agent có khả năng **lưu trữ hội thoại bền vững và hoạt động ổn định trong môi trường Production**.

Kiến trúc cuối cùng:

```text
                    Client
                      |
              conversationId
                      |
                      v
                Load Balancer
                      |
          +-----------+-----------+
          |           |           |
        Pod A       Pod B       Pod C
          |           |           |
          +-----------+-----------+
                      |
          MessageChatMemoryAdvisor
                      |
                JdbcChatMemory
                      |
                 JdbcTemplate
                      |
                      v
                    MySQL
                      |
              Persistent History
```

Cơ chế quan trọng nhất là **`conversationId`**. Mỗi cuộc hội thoại có một ID duy nhất và mọi Pod đều sử dụng ID đó để truy xuất cùng một lịch sử từ MySQL.

Nhờ vậy, hệ thống không phụ thuộc vào bộ nhớ của một JVM cụ thể, không mất context khi Load Balancer chuyển request sang Pod khác và không mất lịch sử khi Pod restart.

Đây là nền tảng phù hợp để triển khai AI Booking Agent theo mô hình **stateless application + persistent shared memory**, đáp ứng tốt yêu cầu scale-out của môi trường Kubernetes.