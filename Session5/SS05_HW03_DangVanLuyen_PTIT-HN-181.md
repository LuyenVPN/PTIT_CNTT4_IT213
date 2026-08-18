# BÀI 3: Đọc hiểu & Dò lỗi - Lập trình phòng thủ chống ảo tưởng tham số

## 1. Phân tích các lỗi logic và điểm yếu

Đoạn code ban đầu có chức năng kiểm tra phòng trống nhưng chưa áp dụng đầy đủ nguyên tắc **Defensive Programming**. Dữ liệu được cung cấp bởi LLM có thể không đầy đủ, sai định dạng hoặc sai logic nghiệp vụ, vì vậy không thể tin tưởng trực tiếp các tham số đầu vào.

### 1.1. Sử dụng nhiều tham số String riêng lẻ

Phương thức ban đầu:

```java
public String getRoomAvailability(
        String checkIn,
        String checkOut,
        String roomType
)
```

có ba tham số `String` độc lập.

Cách thiết kế này có một số nhược điểm:

* Không thể hiện rõ cấu trúc dữ liệu của request.
* Không cung cấp schema đầu vào đủ rõ ràng cho LLM.
* Dễ xảy ra thiếu tham số hoặc truyền nhầm dữ liệu.
* Khó mở rộng khi sau này cần thêm thông tin như số lượng khách, số phòng hoặc mã đặt phòng.

Đặc biệt trong Spring AI Function Calling, việc sử dụng một Java Record như `RoomCheckRequest` giúp framework có thể xây dựng schema có cấu trúc rõ ràng hơn để LLM biết chính xác Tool cần những trường nào.

---

### 1.2. Nguy cơ NullPointerException

Code ban đầu thực hiện:

```java
LocalDate start = LocalDate.parse(checkIn);
LocalDate end = LocalDate.parse(checkOut);
```

Nếu LLM truyền:

```text
checkIn = null
checkOut = null
```

thì quá trình parse có thể phát sinh exception.

Ngoài ra, đoạn:

```java
"Deluxe".equalsIgnoreCase(roomType)
```

cũng không an toàn nếu thay đổi cách viết thành:

```java
roomType.equalsIgnoreCase("Deluxe")
```

vì `roomType` có thể là `null`.

Do đó, cần kiểm tra null trước khi xử lý dữ liệu.

---

### 1.3. Nguy cơ DateTimeParseException

`LocalDate.parse()` mặc định yêu cầu chuỗi ngày có định dạng ISO:

```text
yyyy-MM-dd
```

Ví dụ hợp lệ:

```text
2026-07-15
```

Nhưng LLM có thể trả về:

```text
15-07-2026
```

hoặc:

```text
15/07/2026
```

hoặc:

```text
tomorrow
```

Khi đó:

```java
LocalDate.parse(checkIn)
```

sẽ thất bại.

Vì vậy, cần kiểm tra định dạng bằng Regex trước khi thực hiện parse.

Tuy nhiên, Regex chỉ kiểm tra **hình dạng chuỗi**, không đảm bảo ngày thực sự tồn tại.

Ví dụ:

```text
2026-99-99
```

có thể phù hợp với một Regex đơn giản nhưng vẫn không phải ngày hợp lệ.

Do đó cần thực hiện cả hai bước:

```text
Regex validation
       ↓
LocalDate.parse()
       ↓
Kiểm tra ngày thực tế
```

---

### 1.4. Không kiểm tra đầy đủ logic nghiệp vụ

Code hiện tại chỉ kiểm tra:

```java
if (start.isAfter(end))
```

Điều này chưa đủ.

Cần xác định rõ nghiệp vụ:

* `checkIn` không được null.
* `checkOut` không được null.
* `roomType` không được null hoặc blank.
* Ngày phải đúng định dạng.
* Ngày phải tồn tại.
* Ngày nhận phòng phải trước ngày trả phòng.

Nếu:

```text
checkIn = 2026-08-20
checkOut = 2026-08-20
```

thì `start.isAfter(end)` trả về `false`, nhưng hai ngày bằng nhau có thể không hợp lệ đối với nghiệp vụ lưu trú.

Do đó nên kiểm tra:

```java
if (!start.isBefore(end))
```

để đảm bảo:

```text
checkIn < checkOut
```

---

### 1.5. Ném Exception làm gián đoạn Agent

Đoạn code ban đầu:

```java
throw new IllegalArgumentException(
    "Ngày nhận phòng không thể sau ngày trả phòng."
);
```

là cách xử lý không phù hợp với một Tool được AI Agent gọi trong quá trình hội thoại.

Thay vì ném Exception, Tool nên trả về một kết quả có cấu trúc cho biết thao tác thất bại.

Ví dụ:

```json
{
  "isSuccess": false,
  "isAvailable": false,
  "pricePerNight": 0,
  "message": "Ngày nhận phòng phải trước ngày trả phòng."
}
```

LLM có thể đọc `message`, hiểu nguyên nhân và yêu cầu người dùng cung cấp lại thông tin.

---

# 2. Giải trình giải pháp Validate dữ liệu phòng thủ

Giải pháp refactor áp dụng nhiều lớp kiểm tra theo nguyên tắc:

> **Không tin tưởng dữ liệu đầu vào từ LLM.**

Luồng validation:

```text
LLM
 ↓
RoomCheckRequest
 ↓
Kiểm tra null
 ↓
Kiểm tra blank
 ↓
Regex kiểm tra định dạng
 ↓
LocalDate.parse()
 ↓
Kiểm tra logic ngày
 ↓
Kiểm tra roomType
 ↓
Thực hiện nghiệp vụ
 ↓
RoomCheckResponse
```

## 2.1. Đóng gói request bằng Record

Thay vì:

```java
String checkIn,
String checkOut,
String roomType
```

sử dụng:

```java
public record RoomCheckRequest(
    String checkIn,
    String checkOut,
    String roomType
) {}
```

Điều này giúp mô hình hóa request thành một object có cấu trúc rõ ràng.

Spring AI có thể sử dụng cấu trúc của Record để tạo thông tin schema cho Tool Calling, giúp LLM hiểu rõ các trường cần cung cấp.

---

## 2.2. Đóng gói response bằng Record

Kết quả được chuẩn hóa:

```java
public record RoomCheckResponse(
    boolean isSuccess,
    boolean isAvailable,
    double pricePerNight,
    String message
) {}
```

Trong đó:

| Trường          | Ý nghĩa                                    |
| --------------- | ------------------------------------------ |
| `isSuccess`     | Tool có xử lý request thành công hay không |
| `isAvailable`   | Phòng có còn trống hay không               |
| `pricePerNight` | Đơn giá phòng mỗi đêm                      |
| `message`       | Thông báo chi tiết cho AI và người dùng    |

Việc trả về object có cấu trúc giúp AI dễ dàng phân biệt giữa **lỗi validation** và **kết quả nghiệp vụ**.

---

## 2.3. Kiểm tra null và blank

Trước khi xử lý:

```java
if (request == null) {
    return errorResponse("Dữ liệu yêu cầu không được để trống.");
}
```

Sau đó kiểm tra từng trường:

```java
if (request.checkIn() == null || request.checkIn().isBlank()) {
    ...
}
```

Điều này ngăn dữ liệu không hợp lệ đi sâu vào tầng xử lý.

---

## 2.4. Kiểm tra Regex trước khi parse

Regex được sử dụng để kiểm tra cấu trúc:

```text
^\d{4}-\d{2}-\d{2}$
```

Chỉ chấp nhận dạng:

```text
2026-08-18
```

Không chấp nhận:

```text
18-08-2026
18/08/2026
tomorrow
```

Sau đó mới sử dụng:

```java
LocalDate.parse()
```

để kiểm tra ngày thực tế.

---

## 2.5. Không ném Exception đối với dữ liệu người dùng/LLM

Thay vì:

```java
throw new IllegalArgumentException(...)
```

phương thức trả về:

```java
new RoomCheckResponse(
    false,
    false,
    0,
    "Ngày nhận phòng phải trước ngày trả phòng."
)
```

Nhờ vậy Tool vẫn hoàn thành một lần gọi hợp lệ ở cấp giao thức và AI có thể sử dụng thông tin lỗi để tiếp tục hội thoại.

---

# 3. Mã nguồn Java sau khi refactor

## 3.1. RoomCheckRequest

```java
public record RoomCheckRequest(
        String checkIn,
        String checkOut,
        String roomType
) {
}
```

---

## 3.2. RoomCheckResponse

```java
public record RoomCheckResponse(
        boolean isSuccess,
        boolean isAvailable,
        double pricePerNight,
        String message
) {
}
```

---

## 3.3. BookingService

```java
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.regex.Pattern;

@Service
public class BookingService {

    private static final Pattern DATE_PATTERN =
            Pattern.compile("^\\d{4}-\\d{2}-\\d{2}$");

    private static final double DELUXE_PRICE = 100.0;

    @Tool(
        description = """
        Kiểm tra tình trạng phòng khách sạn.
        Nhận ngày nhận phòng và ngày trả phòng theo định dạng yyyy-MM-dd
        cùng với loại phòng. Nếu dữ liệu không hợp lệ, trả về
        isSuccess=false và message mô tả rõ lỗi để AI yêu cầu người dùng
        cung cấp lại thông tin.
        """
    )
    public RoomCheckResponse getRoomAvailability(
            RoomCheckRequest request
    ) {

        // 1. Kiểm tra request null
        if (request == null) {
            return errorResponse(
                    "Dữ liệu yêu cầu không được để trống."
            );
        }

        // 2. Kiểm tra checkIn
        if (request.checkIn() == null ||
                request.checkIn().isBlank()) {

            return errorResponse(
                    "Thiếu ngày nhận phòng (checkIn). " +
                    "Vui lòng cung cấp ngày nhận phòng theo định dạng yyyy-MM-dd."
            );
        }

        // 3. Kiểm tra checkOut
        if (request.checkOut() == null ||
                request.checkOut().isBlank()) {

            return errorResponse(
                    "Thiếu ngày trả phòng (checkOut). " +
                    "Vui lòng cung cấp ngày trả phòng theo định dạng yyyy-MM-dd."
            );
        }

        // 4. Kiểm tra roomType
        if (request.roomType() == null ||
                request.roomType().isBlank()) {

            return errorResponse(
                    "Thiếu loại phòng (roomType). " +
                    "Vui lòng cung cấp loại phòng, ví dụ Deluxe hoặc Standard."
            );
        }

        // 5. Kiểm tra format ngày bằng Regex
        if (!DATE_PATTERN.matcher(request.checkIn()).matches()) {

            return errorResponse(
                    "Ngày nhận phòng không đúng định dạng. " +
                    "Định dạng yêu cầu là yyyy-MM-dd, ví dụ 2026-08-20."
            );
        }

        if (!DATE_PATTERN.matcher(request.checkOut()).matches()) {

            return errorResponse(
                    "Ngày trả phòng không đúng định dạng. " +
                    "Định dạng yêu cầu là yyyy-MM-dd, ví dụ 2026-08-23."
            );
        }

        // 6. Parse ngày an toàn
        LocalDate start;
        LocalDate end;

        try {
            start = LocalDate.parse(request.checkIn());
            end = LocalDate.parse(request.checkOut());

        } catch (DateTimeParseException e) {

            return errorResponse(
                    "Ngày nhận phòng hoặc ngày trả phòng không hợp lệ. " +
                    "Vui lòng cung cấp ngày thực tế theo định dạng yyyy-MM-dd."
            );
        }

        // 7. Kiểm tra logic nghiệp vụ
        if (!start.isBefore(end)) {

            return errorResponse(
                    "Ngày nhận phòng phải trước ngày trả phòng."
            );
        }

        // 8. Chuẩn hóa roomType
        String roomType = request.roomType().trim();

        // 9. Logic kiểm tra phòng
        boolean isAvailable =
                "Deluxe".equalsIgnoreCase(roomType);

        double pricePerNight =
                isAvailable ? DELUXE_PRICE : 0.0;

        // 10. Trả kết quả thành công
        if (isAvailable) {

            return new RoomCheckResponse(
                    true,
                    true,
                    pricePerNight,
                    "Phòng Deluxe còn phòng trống."
            );
        }

        return new RoomCheckResponse(
                true,
                false,
                0.0,
                "Loại phòng " + roomType +
                " hiện không còn phòng trống."
        );
    }

    private RoomCheckResponse errorResponse(String message) {

        return new RoomCheckResponse(
                false,
                false,
                0.0,
                message
        );
    }
}
```

---

# 4. Ví dụ kết quả khi dữ liệu không hợp lệ

## Trường hợp 1: Thiếu ngày checkout

LLM gửi:

```json
{
  "checkIn": "2026-08-20",
  "checkOut": null,
  "roomType": "Deluxe"
}
```

Tool không crash mà trả về:

```json
{
  "isSuccess": false,
  "isAvailable": false,
  "pricePerNight": 0.0,
  "message": "Thiếu ngày trả phòng (checkOut). Vui lòng cung cấp ngày trả phòng theo định dạng yyyy-MM-dd."
}
```

AI có thể tiếp tục hỏi:

> "Bạn vui lòng cho tôi biết ngày trả phòng nhé."

---

## Trường hợp 2: Sai định dạng ngày

LLM gửi:

```json
{
  "checkIn": "15-07-2026",
  "checkOut": "18-07-2026",
  "roomType": "Deluxe"
}
```

Tool trả:

```json
{
  "isSuccess": false,
  "isAvailable": false,
  "pricePerNight": 0.0,
  "message": "Ngày nhận phòng không đúng định dạng. Định dạng yêu cầu là yyyy-MM-dd, ví dụ 2026-07-15."
}
```

---

## Trường hợp 3: Ngày nhận phòng sau ngày trả phòng

LLM gửi:

```json
{
  "checkIn": "2026-08-25",
  "checkOut": "2026-08-20",
  "roomType": "Deluxe"
}
```

Tool trả:

```json
{
  "isSuccess": false,
  "isAvailable": false,
  "pricePerNight": 0.0,
  "message": "Ngày nhận phòng phải trước ngày trả phòng."
}
```

Không có `IllegalArgumentException` được ném ra.

---

# 5. Kết luận

Bản refactor áp dụng nguyên tắc **Defensive Programming** bằng cách không tin tưởng tuyệt đối dữ liệu do LLM sinh ra.

Các lớp bảo vệ gồm:

```text
RoomCheckRequest
      ↓
Null / Blank Validation
      ↓
Regex Date Validation
      ↓
LocalDate.parse()
      ↓
Business Date Validation
      ↓
Room Availability Logic
      ↓
RoomCheckResponse
```

Điểm quan trọng nhất là **Tool không ném Exception đối với dữ liệu đầu vào không hợp lệ**. Thay vào đó, mọi lỗi được chuyển thành `RoomCheckResponse` với:

```text
isSuccess = false
isAvailable = false
pricePerNight = 0
message = mô tả lỗi cụ thể
```

Nhờ vậy, Spring AI Agent có thể đọc `message`, hiểu vấn đề và tiếp tục hội thoại để yêu cầu người dùng đính chính thông tin thay vì làm gián đoạn toàn bộ luồng xử lý.

Giải pháp này giúp hệ thống **an toàn hơn trước hallucination của LLM, giảm lỗi HTTP 500 và tạo ra Tool có contract đầu vào/đầu ra rõ ràng, phù hợp với kiến trúc AI Agent trong môi trường doanh nghiệp.**