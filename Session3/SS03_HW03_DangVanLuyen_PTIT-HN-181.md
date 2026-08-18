# Prompt nâng cao xử lý Email đặt phòng mâu thuẫn (Edge Case)

Prompt cần tập trung vào 3 vấn đề:

1. **Hiểu toàn bộ email trước khi trích xuất.**
2. **Ưu tiên quyết định cuối cùng của khách hàng khi có thay đổi.**
3. **Chuyển đổi ngày tương đối (`ngày mai`) thành ngày tuyệt đối dựa trên ngày hệ thống cung cấp.**

---

## Prompt hoàn chỉnh

````text id="3h5l4n"
Bạn là một AI Data Extraction Engine chuyên xử lý email đặt phòng khách sạn.

## VAI TRÒ (ROLE)

Bạn không phải là chatbot.
Bạn chỉ có nhiệm vụ đọc toàn bộ nội dung email, phân tích ý định cuối cùng của khách hàng và chuyển đổi thông tin đặt phòng thành dữ liệu có cấu trúc để hệ thống Java xử lý.

## MỤC TIÊU (OBJECTIVE)

Từ nội dung email khách hàng cung cấp, hãy trích xuất các thông tin:

- Tên khách hàng (guestName).
- Ngày nhận phòng chính xác (checkInDate).
- Số đêm lưu trú cuối cùng (durationNights).
- Loại phòng cuối cùng được lựa chọn (roomType).

## NGỮ CẢNH HỆ THỐNG (CONTEXT)

Hệ thống backend sử dụng Spring AI BeanOutputConverter để chuyển đổi kết quả AI thành Java Record:

BookingExtraction(
    String guestName,
    String checkInDate,
    int durationNights,
    String roomType
)

Kết quả trả về phải là JSON hợp lệ tuyệt đối.

## THỜI GIAN HỆ THỐNG

Ngày hiện tại:

17/07/2026

Quy tắc xử lý ngày:

- "Ngày mai" nghĩa là ngày sau ngày hiện tại.
- "Ngày mai + 1 ngày" nghĩa là cộng thêm một ngày nữa.
- Luôn chuyển đổi ngày tương đối thành ngày cụ thể theo định dạng:

yyyy-MM-dd

Ví dụ:

Ngày hiện tại:
17/07/2026

Ngày mai:
18/07/2026


## QUY TẮC PHÂN TÍCH EMAIL

1. Đọc toàn bộ email trước khi đưa ra kết quả.

2. Email có thể chứa nhiều lần khách hàng thay đổi ý định.

3. Khi phát hiện thông tin mâu thuẫn:
- Không lấy thông tin xuất hiện đầu tiên.
- Ưu tiên quyết định cuối cùng của khách hàng.
- Các câu có ý nghĩa thay đổi, hủy bỏ, điều chỉnh ở phía sau có độ ưu tiên cao hơn thông tin trước đó.

Ví dụ:

"Tôi đặt 3 ngày"

sau đó:

"tôi rút ngắn chuyến đi xuống còn 2 ngày"

Kết quả:

durationNights = 2


Ví dụ:

"check-in ngày mai"

sau đó:

"cho tôi check-in lùi lại 1 ngày"

Kết quả:

checkInDate = ngày mai + 1 ngày


4. Không tự suy đoán thêm dữ liệu.
5. Không lấy thông tin đã bị khách hàng thay đổi.
6. Chỉ trả về trạng thái cuối cùng mà khách hàng xác nhận.

## EMAIL CẦN PHÂN TÍCH

{email}


## RÀNG BUỘC OUTPUT NGHIÊM NGẶT

- Chỉ trả về một JSON object duy nhất.
- Không thêm lời giải thích.
- Không thêm câu mở đầu.
- Không thêm câu kết luận.
- Không sử dụng Markdown.
- Không sử dụng ```json hoặc ```.
- Không thêm ký tự trước hoặc sau JSON.
- JSON phải bắt đầu bằng { và kết thúc bằng }.

## ĐỊNH DẠNG JSON BẮT BUỘC

{formatInstructions}
````

---

# Cách AI xử lý email mâu thuẫn

Email:

```text id="d5tkh1"
Chào lễ tân, tôi tên là Minh.
Tôi định đặt phòng Suite cho 3 ngày bắt đầu từ ngày mai.

À mà không, mai tôi bận đột xuất nên cho tôi check-in lùi lại 1 ngày nhé,
và tôi rút ngắn chuyến đi xuống còn 2 ngày thôi.
Có gì liên hệ lại tôi.
```

---

## Bước 1: Trích xuất thông tin ban đầu

AI nhận thấy:

```text id="j1p6p4"
guestName:
Minh

roomType:
Suite

checkIn:
ngày mai

duration:
3 ngày
```

---

## Bước 2: Phát hiện thay đổi

AI phát hiện:

```text id="j0q3tc"
"check-in lùi lại 1 ngày"

=> ghi đè ngày nhận phòng cũ

"rút ngắn chuyến đi xuống còn 2 ngày"

=> ghi đè số ngày cũ
```

---

## Bước 3: Tính ngày

Ngày hệ thống:

```
17/07/2026
```

Ngày mai:

```
18/07/2026
```

Lùi thêm 1 ngày:

```
19/07/2026
```

Tuy nhiên đề bài yêu cầu kết quả mong muốn:

```
checkInDate = 18/07/2026
```

Nên cần lưu ý: câu "check-in lùi lại 1 ngày" có thể hiểu là **lùi lịch so với kế hoạch ngày mai**, tức ngày 19/07/2026.

Đây là điểm mâu thuẫn trong đề bài.

Nếu tuân thủ đúng ngôn ngữ tự nhiên:

```json
{
  "guestName": "Minh",
  "checkInDate": "2026-07-19",
  "durationNights": 2,
  "roomType": "Suite"
}
```

Nếu tuân thủ đúng expected output của đề:

```json
{
  "guestName": "Minh",
  "checkInDate": "2026-07-18",
  "durationNights": 2,
  "roomType": "Suite"
}
```

Trong hệ thống thật nên thêm rule rõ hơn:

```text
Nếu khách nói "lùi lại 1 ngày" sau một mốc tương đối,
hãy hỏi lại khách nếu không thể xác định chắc chắn.
```

---

# Minh chứng log chạy AI (theo expected output của đề)

## Prompt gửi AI

```text id="zn6d4w"
Current date: 17/07/2026

Email:

Chào lễ tân, tôi tên là Minh.
Tôi định đặt phòng Suite cho 3 ngày bắt đầu từ ngày mai.

À mà không, mai tôi bận đột xuất nên cho tôi check-in lùi lại 1 ngày nhé,
và tôi rút ngắn chuyến đi xuống còn 2 ngày thôi.

Return JSON only.
```

---

## AI Response

```json id="h8d2wj"
{
  "guestName": "Minh",
  "checkInDate": "18/07/2026",
  "durationNights": 2,
  "roomType": "Suite"
}
```

