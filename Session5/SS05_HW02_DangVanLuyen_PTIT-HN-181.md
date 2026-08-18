# BÀI 2: Tối ưu Prompt - Tránh bẫy thời gian tương đối cho AI Agent

## 1. System Prompt thiết kế

System Prompt được thiết kế để cung cấp cho AI một mốc thời gian hệ thống chính xác và hướng dẫn AI quy đổi các biểu thức thời gian tương đối như "hôm nay", "ngày mai", "3 ngày" thành ngày cụ thể theo định dạng `yyyy-MM-dd`.

```text
VAI TRÒ:
Bạn là AI Booking Agent của R-Hotels, có nhiệm vụ hỗ trợ khách hàng kiểm tra phòng trống và đặt phòng khách sạn.

NHIỆM VỤ:
- Phân tích yêu cầu đặt phòng của khách hàng.
- Xác định loại phòng, ngày nhận phòng và ngày trả phòng.
- Khi khách sử dụng thời gian tương đối như "hôm nay", "ngày mai", "ngày kia", phải quy đổi thành ngày cụ thể.
- Không được truyền trực tiếp các chuỗi "hôm nay", "ngày mai", "tomorrow", "3 ngày" vào Tool.

NGỮ CẢNH THỜI GIAN:
- Ngày hiện tại của máy chủ là: {currentDate}
- Đây là mốc thời gian tham chiếu duy nhất để tính toán ngày tương đối.
- "Hôm nay" = currentDate.
- "Ngày mai" = currentDate + 1 ngày.
- "Ngày kia" = currentDate + 2 ngày.
- Nếu khách nói "từ ngày mai trong 3 ngày", ngày nhận phòng là ngày mai và ngày trả phòng được tính dựa trên 3 ngày lưu trú.

RÀNG BUỘC:
- Luôn sử dụng currentDate làm mốc tính toán.
- Không tự bịa ra ngày hiện tại.
- Không sử dụng ngày được suy đoán từ kiến thức của model.
- Không truyền các giá trị thời gian tương đối trực tiếp cho Tool.
- Trước khi gọi Tool, phải chuyển tất cả ngày tháng sang ngày cụ thể.

ĐỊNH DẠNG:
- Tất cả ngày tháng truyền vào Tool phải có định dạng chính xác: yyyy-MM-dd.
- Ví dụ: 2026-08-19.
- Nếu cần gọi getRoomAvailability, phải truyền checkInDate và checkOutDate dưới dạng ngày cụ thể theo định dạng trên.
```

Trong đó `{currentDate}` sẽ được thay thế bằng ngày thực tế của máy chủ trước khi gửi request đến AI.

---

# 2. Mã nguồn Java Controller sau khi tối ưu

```java
@RestController
@RequestMapping("/api/booking")
public class BookingController {

    private final ChatClient chatClient;
    private final BookingService bookingService;

    public BookingController(ChatClient.Builder builder,
                             BookingService bookingService) {
        this.bookingService = bookingService;

        this.chatClient = builder
                .defaultTools(bookingService)
                .build();
    }

    @GetMapping("/check")
    public String checkRoom(@RequestParam String message) {

        LocalDate currentDate = LocalDate.now();

        String systemPrompt = """
                VAI TRÒ:
                Bạn là AI Booking Agent của R-Hotels.
                Nhiệm vụ của bạn là hỗ trợ khách hàng kiểm tra phòng trống.

                NHIỆM VỤ:
                - Phân tích yêu cầu đặt phòng của khách hàng.
                - Xác định loại phòng, ngày nhận phòng và ngày trả phòng.
                - Quy đổi tất cả thời gian tương đối thành ngày cụ thể.
                - Không được truyền trực tiếp "hôm nay", "ngày mai",
                  "tomorrow", "3 ngày"... vào Tool.

                NGỮ CẢNH THỜI GIAN:
                - Ngày hiện tại của máy chủ là: %s
                - Đây là mốc thời gian duy nhất để tính toán ngày tương đối.
                - "Hôm nay" = ngày hiện tại.
                - "Ngày mai" = ngày hiện tại + 1 ngày.
                - "Ngày kia" = ngày hiện tại + 2 ngày.

                RÀNG BUỘC:
                - Không được tự bịa ngày hiện tại.
                - Không sử dụng ngày hiện tại từ kiến thức của model.
                - Phải quy đổi ngày tương đối thành ngày cụ thể trước khi gọi Tool.
                - Tất cả ngày truyền vào Tool phải đúng định dạng yyyy-MM-dd.

                ĐỊNH DẠNG:
                - checkInDate: yyyy-MM-dd
                - checkOutDate: yyyy-MM-dd

                Ví dụ:
                Nếu ngày hiện tại là 2026-08-18 và khách yêu cầu
                "phòng Deluxe từ ngày mai trong 3 ngày",
                thì ngày nhận phòng là 2026-08-19.
                Ngày trả phòng được tính theo 3 ngày lưu trú và phải
                được truyền cho Tool dưới dạng yyyy-MM-dd.
                """.formatted(currentDate);

        return this.chatClient
                .prompt()
                .system(systemPrompt)
                .user(message)
                .call()
                .content();
    }
}
```

Cần import:

```java
import java.time.LocalDate;
```

---

# 3. Phân tích & Lập luận kỹ thuật

## 3.1. Vấn đề của Controller ban đầu

Controller ban đầu sử dụng System Prompt cố định:

```java
.defaultSystem(
    "Bạn là trợ lý đặt phòng khách sạn. Hãy giúp khách hàng kiểm tra phòng trống."
)
```

Prompt này không cung cấp cho LLM thông tin về ngày hiện tại của hệ thống.

Khi khách hàng gửi:

```text
Tôi muốn tìm một phòng Deluxe từ ngày mai trong 3 ngày.
```

LLM phải hiểu `"ngày mai"` dựa trên một mốc thời gian. Tuy nhiên, model không nên được xem là nguồn dữ liệu thời gian thực của server.

Nếu không cung cấp mốc thời gian, model có thể:

* Suy đoán ngày hiện tại không chính xác.
* Truyền trực tiếp `"tomorrow"` hoặc `"ngày mai"` vào Tool.
* Sinh ra ngày sai định dạng.
* Tạo arguments không phù hợp với kiểu dữ liệu mà Java yêu cầu.

Nếu backend sử dụng:

```java
LocalDate.parse(checkInDate)
```

với giá trị:

```text
"tomorrow"
```

Java có thể phát sinh:

```text
DateTimeParseException
```

và nếu exception không được xử lý thì request có thể trả về HTTP 500.

---

## 3.2. Cơ chế tiêm `LocalDate.now()`

Trong Controller mới:

```java
LocalDate currentDate = LocalDate.now();
```

lấy ngày hiện tại trực tiếp từ máy chủ.

Sau đó ngày này được đưa vào System Prompt:

```java
""".formatted(currentDate);
```

Ví dụ tại thời điểm request:

```text
currentDate = 2026-08-18
```

AI sẽ nhận được:

```text
Ngày hiện tại của máy chủ là: 2026-08-18
```

Khi khách nói:

```text
Từ ngày mai
```

AI có mốc tham chiếu rõ ràng:

```text
2026-08-18 + 1 ngày
= 2026-08-19
```

Sau đó AI truyền ngày đã chuẩn hóa cho Tool thay vì truyền chuỗi `"ngày mai"`.

---

## 3.3. Luồng xử lý sau khi tối ưu

```text
Client
  |
  | "Tôi muốn phòng Deluxe từ ngày mai trong 3 ngày"
  v
BookingController
  |
  | LocalDate.now()
  | currentDate = 2026-08-18
  v
System Prompt
  |
  | Cung cấp mốc thời gian 2026-08-18
  v
LLM
  |
  | Quy đổi "ngày mai"
  |       ↓
  | 2026-08-19
  v
Tool Calling
  |
  | checkInDate = "2026-08-19"
  | checkOutDate = "yyyy-MM-dd"
  v
BookingService
  |
  v
Kiểm tra phòng
```

Như vậy, tầng Tool nhận được dữ liệu đã được chuẩn hóa thay vì các chuỗi thời gian tự nhiên.

---

## 3.4. Vì sao cơ chế này loại bỏ lỗi parse ngày tháng?

Cơ chế này **loại bỏ nguyên nhân gây lỗi parse từ việc truyền chuỗi thời gian tương đối**.

Trước khi tối ưu:

```text
"ngày mai"
       ↓
LLM
       ↓
"tomorrow"
       ↓
LocalDate.parse("tomorrow")
       ↓
DateTimeParseException
       ↓
HTTP 500
```

Sau khi tối ưu:

```text
"ngày mai"
       ↓
LLM + currentDate
       ↓
"2026-08-19"
       ↓
LocalDate.parse("2026-08-19")
       ↓
Thành công
```

Do đó, việc đưa `LocalDate.now()` vào System Prompt giúp tạo **mốc thời gian xác định**, đồng thời yêu cầu LLM chuẩn hóa ngày tháng trước khi gọi Tool.

Tuy nhiên, về mặt kỹ thuật, không nên khẳng định rằng Prompt **"loại bỏ hoàn toàn mọi khả năng HTTP 500"**. Prompt giúp loại bỏ lỗi parse do ngày tương đối, nhưng hệ thống vẫn cần validation và exception handling ở backend để bảo vệ trước trường hợp LLM trả dữ liệu sai.

Ví dụ có thể kiểm tra:

```java
try {
    LocalDate checkIn = LocalDate.parse(request.getCheckInDate());
    LocalDate checkOut = LocalDate.parse(request.getCheckOutDate());

    // xử lý nghiệp vụ
} catch (DateTimeParseException e) {
    // trả về lỗi validation phù hợp
}
```

Vì vậy, giải pháp tốt nhất là kết hợp:

**Dynamic System Prompt + Tool Schema/Validation + Backend Exception Handling.**

---

# 4. Kết luận

Việc tiêm `LocalDate.now()` vào System Prompt giúp AI Agent có **mốc thời gian thực tế của server**, từ đó có thể quy đổi chính xác các biểu thức như `"hôm nay"`, `"ngày mai"` hoặc `"ngày kia"` thành ngày cụ thể.

Đồng thời, việc bắt buộc Tool sử dụng định dạng `yyyy-MM-dd` giúp dữ liệu truyền từ LLM xuống Java có cấu trúc thống nhất và tương thích với `LocalDate.parse()`.

Giải pháp này giúp giảm đáng kể lỗi `DateTimeParseException`, tránh việc truyền trực tiếp chuỗi thời gian tương đối vào Tool và làm cho quy trình Function Calling của AI Booking Agent ổn định, dễ kiểm soát hơn.