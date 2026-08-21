# BÀI 1: Phân tích & Lựa chọn - Dimension Mismatch

## Đáp án lựa chọn: C

### Giải thích lý do lựa chọn

Phương án **C là tối ưu nhất** vì giải quyết lỗi Dimension Mismatch mà **không cần thay đổi cấu trúc dữ liệu Production hiện tại**.

Production đang sử dụng:

- `text-embedding-3-small` → vector **1536 chiều**
- PostgreSQL/pgvector → `vector(1536)`
- Các ticket cũ đã được embedding bằng cùng mô hình 1536 chiều.

Do đó, nếu Local sử dụng model 384 chiều như `all-minilm` thì vector sinh ra **không thể ghi vào cột `vector(1536)`**. Hai vector có số chiều khác nhau cũng không thể trực tiếp thực hiện Cosine Similarity với nhau.

Với phương án C, Local sử dụng một embedding model có **cùng số chiều 1536** hoặc sử dụng chính OpenAI Embedding. Nhờ đó, database có thể giữ nguyên:

```text
vector_store.embedding → vector(1536)
```

Điều này mang lại các lợi ích:

1. **Không phá vỡ schema hiện tại:** Không cần thay đổi `vector(1536)` thành `vector(384)`.
2. **Đồng nhất không gian vector:** Các embedding có cùng số chiều và tốt nhất được tạo bởi cùng một mô hình embedding thì mới có thể so sánh ngữ nghĩa một cách đáng tin cậy.
3. **Giảm rủi ro migration:** Không phải liên tục thay đổi schema DB khi chuyển giữa Local và Production.
4. **Dễ quản lý deployment:** Profile chỉ cần thay đổi cấu hình embedding model/API endpoint, còn schema vector store vẫn ổn định.
5. **Đảm bảo RAG/Similarity Search:** Top 3 ticket được tìm ra dựa trên cùng một embedding space với dữ liệu đã lưu.

> **Lưu ý quan trọng:** Chỉ “cùng 1536 chiều” chưa đủ để đảm bảo tương thích ngữ nghĩa. Hai model embedding khác nhau nhưng cùng output 1536 chiều vẫn có thể tạo ra hai vector space khác nhau. Vì vậy, tốt nhất Local và Production nên dùng **cùng model embedding**, hoặc nếu dùng model khác thì phải ingest/index lại dữ liệu bằng model đó.

---

## Phân tích các phương án loại trừ

### Phương án A — Không nên chọn

A thay đổi schema Local:

```sql
vector(1536) → vector(384)
```

và khi Production lại đổi về:

```sql
vector(384) → vector(1536)
```

Cách này có rủi ro lớn vì **schema database bị phụ thuộc vào môi trường và model embedding**.

Nếu Local sử dụng `all-minilm` để tạo dữ liệu 384 chiều rồi sau đó chuyển database sang 1536 chiều, những vector 384 chiều đó **không thể tự nhiên trở thành vector 1536 chiều**. Muốn sử dụng chúng trong Production phải embedding lại toàn bộ dữ liệu bằng model 1536 chiều.

Ngoài ra, việc thay đổi dimension của vector column có thể gây:

- Migration phức tạp.
- Mất hoặc phải tái tạo dữ liệu embedding.
- Downtime hoặc ảnh hưởng deployment.
- Nguy cơ sai lệch dữ liệu giữa các môi trường.
- Khó rollback khi deployment thất bại.

Vì vậy A chỉ là cách **chữa lỗi ở tầng database**, chứ chưa giải quyết đúng vấn đề kiến trúc.

---

### Phương án B — Có thể hoạt động nhưng không tối ưu

B tách thành:

```text
Local:
vector_store_local → vector(384)

Production:
vector_store_prod → vector(1536)
```

Về mặt kỹ thuật, phương án này **có thể chạy được** và có ưu điểm là Local không ảnh hưởng trực tiếp đến dữ liệu Production.

Tuy nhiên, nó tạo ra thêm sự phức tạp về hạ tầng:

- Phải quản lý nhiều bảng.
- Phải quản lý nhiều cấu hình.
- Code phải xác định đúng bảng theo Profile.
- Có nguy cơ phát sinh lỗi khi chuyển môi trường.
- Phải duy trì hai không gian embedding khác nhau.

Quan trọng hơn, nếu mục tiêu của hệ thống là lấy **Top 3 ticket lịch sử đã được giải quyết thành công từ kho dữ liệu Production**, thì vector 384 chiều của `all-minilm` không thể trực tiếp so sánh với vector 1536 chiều của Production.

Do đó B phù hợp hơn nếu doanh nghiệp **cố ý muốn duy trì hai hệ embedding độc lập**, chứ không phải lựa chọn tối ưu cho việc giữ một kho vector thống nhất.

---

## Kết luận

**Chọn phương án C.**

Phương án C giữ nguyên schema `vector(1536)` hiện tại của Production và đảm bảo môi trường Local sử dụng embedding có cùng dimension, tốt nhất là **cùng model embedding với Production**.

Điều này giúp:

- Không phải migration database.
- Không làm hỏng dữ liệu embedding hiện tại.
- Giảm rủi ro khi triển khai giữa các môi trường.
- Đảm bảo khả năng thực hiện Cosine Similarity chính xác.
- Giữ kiến trúc Vector Store đơn giản và nhất quán.

### Tóm tắt

| Phương án | Đánh giá | Lý do |
|---|---|---|
| **A** | ❌ Không chọn | Thay đổi schema theo môi trường, rủi ro migration và phải re-embed dữ liệu |
| **B** | ⚠️ Có thể dùng | Hoạt động được nhưng tăng độ phức tạp và tạo hai embedding space |
| **C** | ✅ **Tối ưu** | Giữ nguyên schema 1536 chiều, đơn giản hóa hạ tầng và đảm bảo tương thích embedding |

**Đáp án cuối cùng: C**