# BÀI 1: Phân tích & Lựa chọn - Thiết kế mô tả Tool (Metadata Description)

## 1. Phương án lựa chọn

**Phương án B**

## 2. Phân tích phương án chọn

Phương án B là phương án tối ưu nhất vì mô tả Tool tập trung trực tiếp vào **mục đích nghiệp vụ, tham số đầu vào, điều kiện sử dụng và kết quả đầu ra**. Đây là những thông tin quan trọng để mô hình LLM có thể lựa chọn đúng Tool và sinh đúng arguments khi thực hiện Function Calling trong Spring AI.

### 2.1. Phân biệt rõ chức năng của hai Tool

Hai Tool có mục đích khác nhau:

* `getRoomAvailability`: kiểm tra phòng có còn trống hay không.
* `calculateTotalPrice`: tính tổng tiền dựa trên loại phòng và số ngày lưu trú.

Phương án B thể hiện rõ sự khác biệt:

```text
getRoomAvailability
→ Kiểm tra trạng thái phòng
→ Dựa vào checkInDate, checkOutDate, roomType
→ Trả về trạng thái và đơn giá mỗi đêm

calculateTotalPrice
→ Tính tổng chi phí
→ Dựa vào roomType và numberOfDays
→ Chỉ gọi sau khi đã xác định loại phòng và số ngày
```

Nhờ đó, khi người dùng hỏi:

> "Khách sạn còn phòng Deluxe từ ngày 20/08 đến 23/08 không?"

LLM có cơ sở để lựa chọn `getRoomAvailability` thay vì `calculateTotalPrice`.

Ngược lại, khi đã biết khách ở phòng Deluxe trong 3 ngày và muốn biết tổng tiền, LLM sẽ lựa chọn `calculateTotalPrice`.

### 2.2. Mô tả rõ các tham số

Phương án B chỉ rõ:

* `checkInDate`: ngày nhận phòng, định dạng `yyyy-MM-dd`.
* `checkOutDate`: ngày trả phòng, định dạng `yyyy-MM-dd`.
* `roomType`: loại phòng, ví dụ `Deluxe`, `Standard`.
* `numberOfDays`: số ngày lưu trú và phải lớn hơn `0`.

Điều này rất quan trọng trong Function Calling vì LLM không chỉ cần lựa chọn Tool mà còn phải tạo arguments phù hợp.

Ví dụ, từ yêu cầu:

> "Tôi muốn đặt phòng Deluxe từ 2026-08-20 đến 2026-08-23."

LLM có thể xác định:

```json
{
  "checkInDate": "2026-08-20",
  "checkOutDate": "2026-08-23",
  "roomType": "Deluxe"
}
```

Sau khi xác định được thời gian lưu trú, Tool tính tiền có thể nhận:

```json
{
  "roomType": "Deluxe",
  "numberOfDays": 3
}
```

### 2.3. Có điều kiện nghiệp vụ giúp giảm gọi Tool sai thứ tự

Mô tả của `calculateTotalPrice` nêu rõ:

> "Công cụ này chỉ được gọi sau khi đã xác định được loại phòng và tổng số ngày lưu trú thực tế."

Điều này giúp LLM hiểu được sự phụ thuộc giữa các bước xử lý.

Workflow hợp lý sẽ là:

```text
User request
      ↓
getRoomAvailability
      ↓
Xác định phòng và đơn giá
      ↓
Xác định số ngày lưu trú
      ↓
calculateTotalPrice
```

Điều này đặc biệt hữu ích trong AI Agent vì Agent có thể phải thực hiện nhiều lần Tool Calling để hoàn thành một yêu cầu.

### 2.4. Mô tả rõ kết quả đầu ra

`getRoomAvailability` được mô tả là:

> "Trả về trạng thái và đơn giá mỗi đêm."

Nhờ đó, LLM biết thông tin nhận được từ Tool có thể được sử dụng cho các bước xử lý tiếp theo, thay vì chỉ biết Tool có chức năng "kiểm tra phòng".

---

## 3. Phân tích các phương án loại trừ

### 3.1. Phương án A - Mô tả quá sơ sài

Phương án A sử dụng các mô tả:

```text
"Check phòng trống khách sạn"
"Tính toán giá tiền phòng"
```

Ưu điểm của phương án này là ngắn gọn và dễ đọc. Tuy nhiên, nó thiếu nhiều thông tin quan trọng đối với LLM.

LLM không được mô tả rõ:

* Tool cần những tham số nào.
* Định dạng ngày tháng.
* `roomType` có những giá trị như thế nào.
* `numberOfDays` phải lớn hơn `0`.
* Tool trả về thông tin gì.
* Khi nào nên gọi Tool này trước Tool kia.

Điều này có thể dẫn đến:

* LLM chọn nhầm Tool.
* Sinh thiếu arguments.
* Truyền ngày tháng sai định dạng.
* Nhầm `numberOfDays` với khoảng thời gian check-in/check-out.
* Gọi `calculateTotalPrice` khi chưa xác định được thông tin phòng.

**Rủi ro:** Function Calling vẫn có thể hoạt động, nhưng độ chính xác phụ thuộc nhiều hơn vào khả năng suy luận của model, làm tăng nguy cơ gọi Tool sai hoặc phải thực hiện lại request.

---

### 3.2. Phương án C - Quá thiên về implementation

Phương án C mô tả các chi tiết kỹ thuật như:

```text
BookingService
MySQL DB
JPA
room_status
đếm số bản ghi
```

Đây là những thông tin triển khai nội bộ và không thực sự cần thiết để LLM quyết định sử dụng Tool.

LLM không cần biết Tool được triển khai bằng:

* JPA hay JDBC.
* MySQL hay PostgreSQL.
* Bảng `room_status`.
* Class `BookingService`.

Thông tin LLM cần biết là:

* Tool làm gì.
* Khi nào sử dụng.
* Nhận tham số gì.
* Tham số có ràng buộc gì.
* Trả về thông tin gì.

Ngoài ra, mô tả:

> "trả về kiểu double đại diện cho tích của đơn giá phòng nhân với số ngày"

cũng thiên về implementation hơn là nghiệp vụ.

**Rủi ro:** Description dài nhưng chứa nhiều thông tin không hữu ích, làm tăng lượng context và noise cho model nhưng không giúp đáng kể trong việc lựa chọn Tool.

---

## 4. So sánh tổng hợp

| Phương án | Đánh giá          | Lý do                                                                    |
| --------- | ----------------- | ------------------------------------------------------------------------ |
| **A**     | ❌ Không tối ưu    | Quá ngắn, thiếu tham số, điều kiện và output                             |
| **B**     | ✅ **Tối ưu nhất** | Rõ nghiệp vụ, tham số, định dạng, điều kiện và output                    |
| **C**     | ❌ Không tối ưu    | Quá thiên về implementation, chứa nhiều thông tin nội bộ không cần thiết |

---

## 5. Kết luận

**Phương án B là phương án tối ưu nhất.**

Description của Tool nên tập trung vào các thông tin mà LLM cần để thực hiện Function Calling chính xác:

1. **Tool làm gì?**
2. **Khi nào nên sử dụng Tool?**
3. **Tool cần những tham số nào?**
4. **Các tham số có ràng buộc hoặc định dạng gì?**
5. **Tool trả về thông tin gì?**

Không nên mô tả quá sơ sài như phương án A hoặc đưa quá nhiều chi tiết implementation nội bộ như phương án C.

Vì vậy, **Phương án B giúp LLM phân biệt chính xác `getRoomAvailability` và `calculateTotalPrice`, đồng thời tăng khả năng trích xuất đúng tham số và thực hiện Tool Calling theo đúng trình tự nghiệp vụ.**