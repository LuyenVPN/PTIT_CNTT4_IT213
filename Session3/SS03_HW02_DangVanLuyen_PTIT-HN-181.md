# Báo cáo phân tích lỗi Endpoint Stream “giả SSE”

## 1. Phân tích nguyên nhân lỗi

Đoạn code ban đầu:

```java
@GetMapping("/api/v1/ai/stream")
public Flux<String> getStreamResponse(@RequestParam String message) {
    return chatModel.stream(new Prompt(message))
            .map(response -> response.getResult().getOutput().getText());
}
```

Về mặt reactive, method đã trả về `Flux<String>` nên nhìn qua có vẻ là streaming. Tuy nhiên, endpoint này **chưa chắc là SSE thực sự**.

Các lỗi chính:

---

## Lỗi 1: Thiếu `produces = MediaType.TEXT_EVENT_STREAM_VALUE`

Controller chưa khai báo response type là SSE:

```java
produces = MediaType.TEXT_EVENT_STREAM_VALUE
```

Spring không biết client muốn nhận dữ liệu theo chuẩn Server-Sent Events.

Khi không có cấu hình này, Spring có thể xử lý response như một HTTP response thông thường:

```
Request
   |
   |
Controller
   |
   |
Flux<String>
   |
   |
Buffer toàn bộ response
   |
   |
Trả về client
```

Kết quả:

* LLM mất 20 giây sinh xong.
* Client nhận toàn bộ câu trả lời một lần.
* Không thấy chữ xuất hiện từng phần.

---

## Lỗi 2: Mapping chunk chưa an toàn

Đoạn:

```java
.map(response -> response.getResult().getOutput().getText());
```

có thể gây lỗi nếu một chunk:

* Không có result.
* Không có output.
* Text trả về null.

Ví dụ:

```java
response.getResult()
        .getOutput()
        .getText()
```

Nếu một token stream không chứa text:

```
response
 |
 +-- result = null
```

sẽ gây:

```
NullPointerException
```

---

## Lỗi 3: Chưa kiểm tra model có hỗ trợ streaming

Không phải tất cả ChatModel đều stream.

Ví dụ:

Sai:

```
ChatClient
      |
      |
chatModel.call()
      |
      |
String hoàn chỉnh
```

Đúng:

```
ChatClient
      |
      |
chatModel.stream()
      |
      |
Flux<ChatResponse>
      |
      |
chunk 1
chunk 2
chunk 3
...
```

Nếu bên trong `chatModel.stream()` thực chất gọi API non-stream thì Flux vẫn chỉ phát ra một phần tử duy nhất.

---

# 2. Cơ chế Server-Sent Events (SSE) trong Spring WebFlux

## SSE là gì?

SSE là cơ chế server chủ động đẩy dữ liệu về client qua một HTTP connection duy trì liên tục.

Luồng hoạt động:

```
Client
 |
 | GET /api/v1/ai/stream
 |
 v

Spring WebFlux Controller

 |
 |
 v

Flux<String>

 |
 +----------+
 | chunk 1  |
 +----------+
 |
 +----------+
 | chunk 2  |
 +----------+
 |
 +----------+
 | chunk 3  |
 +----------+

Client nhận từng event
```

---

## Header bắt buộc

Một response SSE chuẩn:

```
Content-Type:
text/event-stream
```

Ví dụ dữ liệu truyền:

```
data: Xin

data: chào

data: bạn
```

Client nhận:

```
Xin
chào
bạn
```

không cần chờ toàn bộ câu trả lời.

---

# 3. Vì sao code cũ bị blocking?

Code cũ:

```java
return chatModel.stream(new Prompt(message))
```

chỉ tạo ra:

```
Flux
```

nhưng HTTP layer chưa biết phải flush từng element.

Không có:

```java
produces = MediaType.TEXT_EVENT_STREAM_VALUE
```

Spring có thể serialize thành một response hoàn chỉnh.

Luồng lỗi:

```
LLM
 |
 | generate 20 giây
 |
Flux collect
 |
HTTP Response
 |
Client nhận tất cả
```

Thay vì:

```
LLM
 |
chunk 1 ----> Client
 |
chunk 2 ----> Client
 |
chunk 3 ----> Client
 |
...
```

---

# 4. Controller SSE chuẩn

Ví dụ hoàn chỉnh:

```java
package com.example.rlogistics.controller;

import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/v1/ai")
public class AiStreamController {


    private final ChatModel chatModel;


    public AiStreamController(ChatModel chatModel) {
        this.chatModel = chatModel;
    }


    @GetMapping(
            value = "/stream",
            produces = MediaType.TEXT_EVENT_STREAM_VALUE
    )
    public Flux<String> streamResponse(
            @RequestParam String message
    ) {


        return chatModel.stream(new Prompt(message))

                .map(ChatResponse::getResult)

                .filter(result -> result != null)

                .map(result -> result.getOutput())

                .filter(output -> output != null)

                .map(output -> output.getText())

                .filter(text -> text != null && !text.isBlank())

                .onErrorResume(
                        exception ->
                                Flux.just(
                                    "AI service error: "
                                    + exception.getMessage()
                                )
                );
    }
}
```

---

# 5. Phiên bản dùng `ServerSentEvent`

Đây là cách rõ ràng hơn khi làm production:

```java
@GetMapping(
        value = "/stream",
        produces = MediaType.TEXT_EVENT_STREAM_VALUE
)
public Flux<ServerSentEvent<String>> stream(
        @RequestParam String message
) {

    return chatModel.stream(new Prompt(message))

            .map(response ->
                    ServerSentEvent.builder(
                            response.getResult()
                                    .getOutput()
                                    .getText()
                    )
                    .build()
            );
}
```

Client nhận:

```
event:
data: Xin

event:
data: chào

event:
data: R-Logistics
```

---

# 6. Kiểm tra bằng Chrome/Postman

Request:

```
GET

http://localhost:8080/api/v1/ai/stream?message=Quy trình thông quan
```

Response đúng:

```
Content-Type:
text/event-stream
```

Dữ liệu xuất hiện:

```
data: Quy

data: trình

data: khai

data: báo

data: hải

data: quan
```
