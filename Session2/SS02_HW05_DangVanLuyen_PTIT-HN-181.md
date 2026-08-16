## 1. Mã nguồn lớp `FeedbackAnalysisService.java`

```java
package com.rlogistics.service;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Service;

@Service
public class FeedbackAnalysisService {

    private final ChatModel chatModel;

    // Inject interface ChatModel thay vì inject trực tiếp OllamaChatModel/OpenAiChatModel
    public FeedbackAnalysisService(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    /**
     * Phân tích phản hồi khách hàng bằng LLM
     *
     * @param feedbackText nội dung phản hồi khách hàng
     * @return kết quả phân tích từ AI
     */
    public String analyzeFeedback(String feedbackText) {

        String prompt = """
                Bạn là hệ thống CSKH của R-Logistics.
                Hãy phân tích phản hồi khách hàng sau:
                
                %s
                
                Hãy đưa ra:
                - Cảm xúc khách hàng
                - Vấn đề chính
                - Đề xuất xử lý
                """.formatted(feedbackText);


        ChatResponse response = chatModel.call(prompt);

        return response
                .getResult()
                .getOutput()
                .getText();
    }
}
```

---

# 2. Vì sao inject `ChatModel` thể hiện thiết kế Loosely Coupled?

Trong thiết kế truyền thống, nếu viết:

```java
private final OllamaChatModel ollamaChatModel;
```

Service sẽ phụ thuộc trực tiếp vào một nhà cung cấp AI cụ thể.

Ví dụ:

```text
FeedbackAnalysisService
          |
          |
          ↓
 OllamaChatModel
```

Khi muốn chuyển sang OpenAI/OpenRouter:

```text
FeedbackAnalysisService
          |
          |
          ↓
 OpenAiChatModel
```

phải sửa code Service.

Điều này tạo ra **High Coupling (liên kết chặt)**.

---

## Cách tiếp cận sử dụng Interface

Spring AI cung cấp interface:

```java
ChatModel
```

Service chỉ biết:

```text
"Tôi cần một đối tượng có khả năng gửi prompt và nhận kết quả"
```

Service không quan tâm implementation bên dưới là:

```
ChatModel
    |
    +-- OllamaChatModel
    |
    +-- OpenAiChatModel
```

Kiến trúc lúc này:

```
              FeedbackAnalysisService
                       |
                       |
                  ChatModel
                   Interface
                       |
        -------------------------------
        |                             |
 OllamaChatModel              OpenAiChatModel
    Local AI                    Cloud AI
```

---

## Lợi ích của Programming to Interface

### 1. Dễ thay đổi nhà cung cấp AI

Ví dụ:

Ban đầu:

```properties
spring.ai.ollama.chat.options.model=qwen2.5-coder:7b
```

Sau đó chuyển sang:

```properties
spring.ai.openai.chat.options.model=gpt-4o
```

Không cần sửa:

```java
FeedbackAnalysisService
```

---

### 2. Dễ kiểm thử

Có thể tạo mock:

```java
ChatModel mockChatModel;
```

để test Service mà không cần chạy Ollama/OpenAI thật.

---

### 3. Tuân thủ nguyên lý Dependency Inversion Principle (DIP)

Trong SOLID:

* Module cấp cao không phụ thuộc module cấp thấp.
* Cả hai phụ thuộc abstraction.

Ở đây:

```
FeedbackAnalysisService
          |
          ↓
      ChatModel
          |
          ↓
Ollama/OpenAI implementation
```

---

# 3. Xử lý xung đột Bean khi có cả Ollama và OpenAI Starter

Khi khai báo:

```gradle
implementation 'org.springframework.ai:spring-ai-starter-model-ollama'

implementation 'org.springframework.ai:spring-ai-starter-model-openai'
```

Spring Boot có thể tạo ra:

```
ChatModel Bean 1
      |
      ↓
OllamaChatModel


ChatModel Bean 2
      |
      ↓
OpenAiChatModel
```

Khi inject:

```java
public FeedbackAnalysisService(ChatModel chatModel)
```

Spring không biết chọn bean nào.

Lỗi:

```
NoUniqueBeanDefinitionException:
expected single matching bean but found 2
```

---

# Cách 1: Sử dụng `@Primary`

Đánh dấu bean mặc định.

Ví dụ muốn ưu tiên Ollama:

```java
@Configuration
public class AiConfig {

    @Bean
    @Primary
    public ChatModel ollamaChatModel(ChatModel ollamaModel) {
        return ollamaModel;
    }
}
```

Khi đó:

```java
@Autowired
private ChatModel chatModel;
```

Spring tự chọn bean có:

```java
@Primary
```

---

Luồng:

```
FeedbackAnalysisService
          |
          ↓
       ChatModel
          |
          ↓
     @Primary Bean
          |
          ↓
    OllamaChatModel
```

---

# Cách 2: Sử dụng `@Profile`

Tạo cấu hình riêng cho từng môi trường.

## Local Profile

```java
@Configuration
@Profile("local")
public class LocalAiConfig {

    @Bean
    public ChatModel localChatModel(
            OllamaChatModel ollamaChatModel) {

        return ollamaChatModel;
    }
}
```

---

## Cloud Profile

```java
@Configuration
@Profile("cloud")
public class CloudAiConfig {

    @Bean
    public ChatModel cloudChatModel(
            OpenAiChatModel openAiChatModel) {

        return openAiChatModel;
    }
}
```

---

Chạy local:

```bash
java -jar app.jar --spring.profiles.active=local
```

Spring tạo:

```
ChatModel
    |
    ↓
OllamaChatModel
```

---

Chạy cloud:

```bash
java -jar app.jar --spring.profiles.active=cloud
```

Spring tạo:

```
ChatModel
    |
    ↓
OpenAiChatModel
```

---

# So sánh hai cách

| Giải pháp  | Khi nên dùng                                          |
| ---------- | ----------------------------------------------------- |
| `@Primary` | Có nhiều bean nhưng luôn muốn một bean mặc định       |
| `@Profile` | Có nhiều môi trường chạy khác nhau (local/cloud/test) |

---

## Kết luận

Việc `FeedbackAnalysisService` phụ thuộc vào `ChatModel` thay vì `OllamaChatModel` hoặc `OpenAiChatModel` giúp hệ thống R-Logistics đạt kiến trúc **loosely coupled**. Service chỉ phụ thuộc vào abstraction, còn việc chọn AI provider được quản lý bởi Spring Container.

Khi triển khai Hybrid AI Runtime, cách phù hợp nhất thường là kết hợp:

* `ChatModel` interface trong tầng Service.
* `@Profile("local")` cho Ollama.
* `@Profile("cloud")` cho OpenRouter/OpenAI.

Nhờ đó có thể chuyển đổi mô hình AI mà không thay đổi logic nghiệp vụ.
