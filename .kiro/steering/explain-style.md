---
inclusion: manual
---

# Phong cách giải thích kỹ thuật

## Nguyên tắc cốt lõi

Khi giải thích bất kỳ khái niệm kỹ thuật nào, luôn theo cấu trúc sau:

1. **Câu chuyện thực tế** — Lấy 1 ví dụ đời sống, không liên quan đến IT
2. **Vấn đề nó giải quyết** — Trước khi có nó, người ta khổ thế nào?
3. **Cơ chế hoạt động** — Nó làm gì bên trong, dùng ngôn ngữ đơn giản
4. **Map vào kỹ thuật** — Giờ mới gắn lại với code / kiến trúc

Không bao giờ bắt đầu bằng định nghĩa kỹ thuật khô khan.
Không dùng jargon nếu chưa giải thích jargon đó là gì.

---

## Ví dụ mẫu — Cách giải thích đúng phong cách này

---

### Zeebe Engine là gì?

**Câu chuyện thực tế:**
Hình dung bạn là người quản lý một nhà hàng lớn. Mỗi đơn gọi món là một "process". Bếp, phục vụ, thu ngân — mỗi người làm 1 việc theo đúng thứ tự. Bạn không thể đứng canh từng bàn, vừa nhớ bàn 5 đang chờ tráng miệng, bàn 12 vừa gọi thêm nước, bàn 3 chưa thanh toán.

Vấn đề: Nếu bạn bị ốm 1 ngày, ai nhớ bàn 5 đang chờ tráng miệng?

**Zeebe Engine = Sổ điều hành nhà hàng thông minh.**
Nó ghi nhớ mọi đơn đang ở bước nào, ai đang làm gì, bước tiếp theo là gì — kể cả khi bạn vắng mặt, kể cả khi điện cúp rồi bật lại.

**Về mặt kỹ thuật:**
Zeebe là distributed workflow engine. Lưu trạng thái của mọi process instance bền vững (persistent). Khi app restart, process không mất — nó tiếp tục từ đúng chỗ đang dừng.

---

### Token là gì?

**Câu chuyện thực tế:**
Đi khám bệnh ở bệnh viện. Bạn lấy số thứ tự — tờ giấy nhỏ ghi "A045". Tờ giấy đó theo bạn qua từng phòng: đăng ký → xét nghiệm → bác sĩ → thu ngân. Mỗi phòng xem số của bạn, làm xong rồi cho bạn đi tiếp.

Đó là Token.

**Về mặt kỹ thuật:**
Token là con trỏ đại diện cho một process instance đang chạy. Nó "đậu" tại node hiện tại trong BPMN. Khi node đó hoàn thành, token di chuyển sang node tiếp theo theo sequence flow. Parallel Gateway nhân bản token — nhiều token = nhiều nhánh chạy song song.

---

### Job Queue / Worker là gì?

**Câu chuyện thực tế:**
Bưu điện nhận thư → đặt vào ô phân loại → nhân viên giao thư đến lấy khi sẵn sàng → giao xong báo lại.

Không phải bưu điện đến tận nhà nhân viên giao thư để đưa thư. Nhân viên chủ động ra lấy khi họ rảnh.

Thêm nhiều nhân viên giao thư → thư được giao nhanh hơn, không cần thay đổi gì ở bưu điện.

**Về mặt kỹ thuật:**
Zeebe tạo Job vào queue khi process instance đến Service Task. Worker (Spring Boot) dùng long polling để lấy job về xử lý. Scale worker = thêm instance Spring Boot, không cần config Zeebe. Loose coupling thật sự.

---

### Long Polling là gì?

**Câu chuyện thực tế:**
Bạn đặt đồ ăn online. Có 2 cách nhà hàng thông báo:

- **Cách tệ (polling thông thường):** Bạn gọi điện mỗi 5 phút hỏi "xong chưa?" — tốn công, tốn đường truyền.
- **Cách tệ hơn (webhook):** Nhà hàng phải biết số điện thoại của bạn trước — nhưng nếu bạn đổi số thì sao?
- **Long polling:** Bạn gọi hỏi 1 lần, nhà hàng nói "chờ tôi chút, tôi sẽ không cúp máy" — khi đồ ăn xong họ thông báo ngay trên đường dây đó.

**Về mặt kỹ thuật:**
Worker gửi request lên Zeebe, Zeebe giữ connection mở (30 giây). Nếu có job → trả về ngay. Nếu không có → timeout → Worker gửi lại request mới. Zeebe không cần biết Worker đang ở đâu, bao nhiêu Worker. Worker tự tìm đến.

---

### Process Variables là gì?

**Câu chuyện thực tế:**
Hồ sơ vay ngân hàng. Từ lúc nộp đến lúc giải ngân, hồ sơ đó đi qua 10 người: CV, Checker, Thẩm định, Phê duyệt... Mỗi người đọc thông tin trong hồ sơ, rồi ghi thêm ý kiến của mình vào — nhưng không xóa thông tin cũ. Đến cuối, hồ sơ chứa đầy đủ dấu vết của mọi người.

**Về mặt kỹ thuật:**
Variables là map key-value gắn với process instance. Mỗi Worker có thể đọc biến hiện có và ghi thêm biến mới. Variables được merge (không replace). Gateway dùng variables để evaluate điều kiện rẽ nhánh (FEEL expression).

---

### Incident là gì?

**Câu chuyện thực tế:**
Dây chuyền sản xuất ô tô. Mọi thứ chạy tự động. Đột nhiên robot hàn bị kẹt. Dây chuyền không tự ý bỏ qua bước hàn — chiếc xe đó dừng lại ngay đó, đèn đỏ bật lên, kỹ sư đến sửa, xong xuôi thì chiếc xe tiếp tục đi. Không ai sản xuất ra xe thiếu mối hàn.

**Về mặt kỹ thuật:**
Khi Worker throw exception và hết retry, Zeebe tạo Incident. Process instance đóng băng tại node đó — không bị cancel, không bị mất dữ liệu. Người vận hành vào Camunda Operate để xem lỗi gì, fix code, deploy lại worker, resolve incident → process tiếp tục từ đúng chỗ đó.

---

### Gateway là gì?

**Câu chuyện thực tế:**
Ngã tư giao thông. Đèn xanh đỏ quyết định xe đi hướng nào dựa trên điều kiện thực tế (giờ cao điểm, hướng nào đông hơn). Xe không tự quyết định — luật giao thông (condition) quyết định.

- **Exclusive Gateway (XOR):** Ngã tư 1 chiều — chỉ 1 hướng được đi tại 1 thời điểm.
- **Parallel Gateway (AND):** Vòng xuyến — xe đi nhiều hướng cùng lúc.
- **Inclusive Gateway (OR):** Có thể đi 1 hoặc nhiều hướng tùy điều kiện.

**Về mặt kỹ thuật:**
Gateway evaluate FEEL expression từ process variables. Exclusive: chọn 1 sequence flow có condition = true. Parallel: kích hoạt tất cả outgoing flows đồng thời. Không cần code Java — Zeebe engine tự xử lý.

---

### Event Sourcing trong Zeebe là gì?

**Câu chuyện thực tế:**
Sổ kế toán ngân hàng. Ngân hàng không lưu "số dư hiện tại = 5 triệu". Họ lưu từng giao dịch: nạp 10 triệu, rút 3 triệu, rút 2 triệu. Số dư = tổng của tất cả giao dịch. Nếu có sai sót → xem lại lịch sử, tìm ra đúng chỗ sai.

**Về mặt kỹ thuật:**
Zeebe không lưu "process instance đang ở bước X". Nó lưu chuỗi events: INSTANCE_CREATED, ELEMENT_ACTIVATED, JOB_CREATED, JOB_COMPLETED, ELEMENT_COMPLETED... State hiện tại = replay toàn bộ event log. Lợi ích: audit trail đầy đủ, không bao giờ mất dữ liệu, có thể debug bằng cách xem lại lịch sử.

---

## Quy tắc áp dụng

Khi được hỏi về bất kỳ khái niệm kỹ thuật nào trong dự án này:

1. **Bắt đầu bằng câu hỏi "Nếu không có nó thì sao?"** — làm nổi bật vấn đề nó giải quyết
2. **Dùng 1 câu chuyện đời thực** — không cần liên quan đến IT, càng quen thuộc càng tốt
3. **Sau đó mới map vào kỹ thuật** — dùng chính ngôn ngữ kỹ thuật chuẩn xác
4. **Không simplify sai** — ví dụ phải đúng bản chất, không được làm sai nghĩa kỹ thuật
5. **Ưu tiên "tại sao" trước "cái gì"** — người học hiểu lý do tồn tại trước khi học cách dùng

## Hỏi trước khi làm.
Trước khi khởi tạo ứng dụng Springboot hoặc Frontend/App hoặc bất cứ gì liên quan đến việc khởi tạo , thay đổi cần được hỏi ý kiến để confirm.
