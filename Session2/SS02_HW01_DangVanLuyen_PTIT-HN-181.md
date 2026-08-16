# Bài 1: Phân tích & Lựa chọn — Cấu hình đa môi trường (Profiles)

## Đáp án tối ưu: Phương án B

Phương án B là lựa chọn tối ưu nhất vì sử dụng đúng cơ chế **Spring Profiles** để tách cấu hình theo từng môi trường. Nhờ đó, hệ thống có thể chuyển đổi giữa mô hình Qwen chạy local thông qua Ollama và Gemini thông qua OpenRouter mà không cần thay đổi mã nguồn Java.

## 1. Phân tích phương án B

Phương án B chia cấu hình thành 3 file:

- `application.properties`: chứa cấu hình chung và xác định profile đang sử dụng.
- `application-local.properties`: chứa cấu hình cho môi trường Development/Testing.
- `application-cloud.properties`: chứa cấu hình cho Staging/Production.

### Cấu hình Local

```properties
# application-local.properties
spring.ai.ollama.base-url=http://localhost:11434
spring.ai.ollama.chat.options.model=qwen2.5-coder:7b
```

Khi:

```properties
spring.profiles.active=local
```

Spring Boot sẽ tự động nạp:

```text
application.properties
        ↓
application-local.properties
        ↓
Ollama
        ↓
Qwen 2.5 Coder
```

### Cấu hình Cloud

```properties
# application-cloud.properties
spring.ai.openai.api-key=${OPENROUTER_API_KEY}
spring.ai.openai.base-url=https://openrouter.ai/api/v1
spring.ai.openai.chat.options.model=google/gemini-2.5-flash
```

Khi chuyển sang:

```properties
spring.profiles.active=cloud
```

Spring Boot sẽ nạp:

```text
application.properties
        ↓
application-cloud.properties
        ↓
OpenRouter
        ↓
Gemini 2.5 Flash
```

Điểm quan trọng là mã nguồn Java không cần thay đổi.

## 2. Vì sao phương án B tối ưu?

### 2.1. Tách biệt cấu hình theo môi trường

Mỗi môi trường có một file cấu hình riêng:

```text
application-local.properties
        → Ollama + Qwen

application-cloud.properties
        → OpenRouter + Gemini
```

Nhờ đó cấu hình của Local và Cloud không bị trộn lẫn. Đây chính là mục đích quan trọng của Spring Profiles.

### 2.2. Giảm nguy cơ conflict Bean

Spring AI có thể tự động cấu hình các `ChatModel` dựa trên các properties tương ứng.

Nếu đồng thời cấu hình:

```properties
spring.ai.ollama.*
```

và:

```properties
spring.ai.openai.*
```

thì nhiều AI provider có thể cùng được auto-configure.

Điều này có thể dẫn đến:

- Nhiều `ChatModel` bean cùng tồn tại.
- Có thể xảy ra lỗi ambiguity khi inject `ChatModel`.
- Có thể phải sử dụng `@Qualifier`.
- Khó xác định model nào đang được sử dụng.

Phương án B tránh được vấn đề này ở mức cấu hình vì mỗi profile chỉ chứa cấu hình của một backend AI.

### 2.3. Không cần sửa mã nguồn Java

Ví dụ Java chỉ cần sử dụng abstraction:

```java
private final ChatModel chatModel;
```

Không cần viết:

```java
if (environment.equals("local")) {
    // sử dụng Ollama
} else {
    // sử dụng OpenRouter
}
```

Khi thay đổi profile, Spring Boot sẽ tự động cung cấp implementation phù hợp.

Ví dụ:

```properties
spring.profiles.active=local
```

sử dụng Ollama.

Đổi thành:

```properties
spring.profiles.active=cloud
```

sử dụng OpenRouter.

### 2.4. Dễ bảo trì

Khi cấu hình được tách thành các profile:

```text
application.properties
application-local.properties
application-cloud.properties
```

việc quản lý hệ thống trở nên rõ ràng hơn.

Developer có thể tập trung vào cấu hình Local:

```properties
spring.ai.ollama.*
```

Trong khi DevOps/Production có thể tập trung vào:

```properties
spring.ai.openai.*
```

Không cần sửa code Java khi chuyển môi trường.

### 2.5. Phù hợp với nguyên tắc Separation of Concerns

Phương án B phân chia trách nhiệm rõ ràng:

```text
Local Profile
    ↓
Ollama
    ↓
Qwen

Cloud Profile
    ↓
OpenRouter
    ↓
Gemini
```

Mỗi file chỉ chịu trách nhiệm cho một môi trường cụ thể.

Điều này giúp hệ thống:

- Dễ đọc.
- Dễ kiểm tra.
- Dễ bảo trì.
- Dễ mở rộng.
- Ít xảy ra lỗi cấu hình ngoài ý muốn.

## 3. Phân tích phương án A

### Cấu hình

```properties
# application.properties
spring.profiles.active=local

spring.ai.ollama.base-url=http://localhost:11434
spring.ai.ollama.chat.options.model=qwen2.5-coder:7b

spring.ai.openai.api-key=${OPENROUTER_API_KEY}
spring.ai.openai.base-url=https://openrouter.ai/api/v1
spring.ai.openai.chat.options.model=google/gemini-2.5-flash
```

### Hạn chế

Phương án A đưa cả cấu hình Ollama và OpenRouter vào cùng một file.

Mặc dù có:

```properties
spring.profiles.active=local
```

nhưng điều này không tự động khiến Spring Boot bỏ qua các properties OpenRouter đang nằm trong `application.properties`.

Do đó, nếu các starter tương ứng được sử dụng, cả hai backend AI có thể được auto-configure.

### Có thể dẫn đến:

```text
Ollama ChatModel
       +
OpenAI-compatible ChatModel
       ↓
Nhiều ChatModel bean
       ↓
Có khả năng xảy ra ambiguity/conflict
```

Ví dụ khi Spring cần inject:

```java
ChatModel chatModel;
```

có thể không xác định được bean nào nên sử dụng nếu có nhiều `ChatModel` cùng loại.

Khi đó có thể phải sử dụng:

```java
@Qualifier(...)
```

hoặc cơ chế lựa chọn model bổ sung.

### Vấn đề về tính đóng gói

Cấu hình Local và Cloud bị trộn vào cùng một file:

```text
application.properties
├── Ollama
└── OpenRouter
```

Điều này làm giảm:

- Tính tách biệt.
- Tính dễ bảo trì.
- Khả năng kiểm soát môi trường.
- Khả năng mở rộng.

Do đó, phương án A không tận dụng đúng lợi ích của Spring Profiles.

## 4. Phân tích phương án C

### Cấu hình

```properties
# application.properties
spring.ai.active-model-type=ollama
spring.ai.ollama.url=http://localhost:11434
spring.ai.openai.url=https://openrouter.ai/api/v1
```

### Lỗi kỹ thuật chính

Phương án C tự đặt các property:

```properties
spring.ai.active-model-type
spring.ai.ollama.url
spring.ai.openai.url
```

Các property này không phải cơ chế chuẩn để Spring AI tự động lựa chọn ChatModel.

Đặc biệt:

```properties
spring.ai.active-model-type=ollama
```

chỉ là một custom property.

Spring AI không tự hiểu rằng:

```text
active-model-type = ollama
```

có nghĩa là:

```text
Hãy sử dụng Ollama ChatModel
```

Nếu muốn sử dụng property này, lập trình viên phải tự viết Java code để đọc giá trị rồi lựa chọn model tương ứng.

Điều này đi ngược yêu cầu của bài toán là chuyển đổi môi trường thông qua configuration mà không cần sửa mã nguồn Java.

### Sai key cấu hình

Phương án C sử dụng:

```properties
spring.ai.ollama.url
```

trong khi cấu hình chuẩn là:

```properties
spring.ai.ollama.base-url
```

Tương tự, việc chỉ khai báo:

```properties
spring.ai.openai.url
```

không đủ để cấu hình đầy đủ OpenAI-compatible provider.

Cần có thêm:

```properties
spring.ai.openai.api-key
spring.ai.openai.base-url
spring.ai.openai.chat.options.model
```

Do đó, C có thể khiến Spring AI không nhận hoặc không sử dụng các cấu hình mong muốn.

## 5. So sánh ba phương án

| Tiêu chí | Phương án A | Phương án B | Phương án C |
|-----------|------------|------------|------------|
| Sử dụng Spring Profiles | ⚠️ Không hiệu quả | ✅ Đúng | ❌ Không |
| Tách Local/Cloud | ❌ | ✅ | ❌ |
| Cấu hình chuẩn Spring AI | ✅ | ✅ | ❌ |
| Giảm nguy cơ conflict ChatModel | ❌ | ✅ | ❌ |
| Không cần sửa Java | ⚠️ | ✅ | ❌ |
| Dễ bảo trì | ⚠️ | ✅ | ❌ |
| Dễ mở rộng | ⚠️ | ✅ | ❌ |
| Phù hợp Production | ⚠️ | ✅ | ❌ |

## 6. Kết luận

**Đáp án đúng: Phương án B.**

Phương án B là tối ưu nhất vì tận dụng đúng cơ chế Spring Profiles để tách biệt cấu hình theo môi trường.

```text
                    Spring Profiles
                          │
             ┌────────────┴────────────┐
             │                         │
          local                      cloud
             │                         │
             ↓                         ↓
          Ollama                   OpenRouter
             │                         │
             ↓                         ↓
           Qwen                    Gemini
```

### Phương án A

Không tối ưu vì cấu hình cả Ollama và OpenRouter trong cùng một file. `spring.profiles.active=local` không tự động loại bỏ các properties OpenRouter, do đó có nguy cơ nhiều `ChatModel` được auto-configure và gây conflict hoặc ambiguity.

### Phương án B

Là lựa chọn tối ưu nhất vì:

- Tách biệt cấu hình theo môi trường.
- Sử dụng đúng Spring Profiles.
- Giảm nguy cơ conflict giữa các AI provider.
- Dễ bảo trì.
- Dễ triển khai.
- Không cần sửa Java code khi chuyển môi trường.
- Phù hợp với nguyên tắc Separation of Concerns.

### Phương án C

Không phù hợp vì sử dụng các property tự đặt tên hoặc sai key chuẩn của Spring AI như:

```properties
spring.ai.active-model-type
spring.ai.ollama.url
spring.ai.openai.url
```

Spring AI không tự động sử dụng `active-model-type` để lựa chọn model. Muốn cơ chế này hoạt động phải viết thêm Java code, trái với yêu cầu của đề bài.

**Vì vậy, phương án B là đáp án tối ưu nhất về mặt kỹ thuật, tính đóng gói và khả năng bảo trì.**