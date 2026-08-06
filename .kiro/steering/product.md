# Product

## Mục tiêu
Học Camunda 8 theo hướng **từ bé đến lớn** (incremental). Bắt đầu từ những ví dụ đơn giản nhất, dần dần nâng cấp đến luồng phê duyệt LPD Tín Chấp thực tế.

## Nguồn tham chiếu
- Tài liệu chính thống Camunda: https://docs.camunda.io/docs
- Database thực tế: `lpd_tinchap`, `pvcb_lpddb` (lpd_schema)

## Chiến lược tiếp cận: Từ bé đến lớn

### Phase 1 — Hello Camunda (CRUD đơn giản)
- Tạo Spring Boot project kết nối Camunda 8
- BPMN đơn giản: Start → 1 Service Task → End
- Worker xử lý task đơn giản (print log)
- React hiển thị danh sách process instances (CRUD cơ bản)
- **Mục tiêu**: Hiểu cách Zeebe client, worker, deploy BPMN hoạt động

### Phase 2 — Human Task cơ bản
- Thêm 1 User Task vào process
- React form đơn giản: submit → complete task
- Hiểu Tasklist API, claim/complete task
- **Mục tiêu**: Hiểu luồng Human Task trong Camunda

### Phase 3 — Gateway & nhiều bước
- BPMN với Exclusive Gateway (if/else)
- 2-3 bước liên tiếp: Tạo → Kiểm tra → Phê duyệt/Từ chối
- Variables truyền giữa các bước
- **Mục tiêu**: Hiểu Gateway, process variables, conditional flow

### Phase 4 — Multi-role Approval (đơn giản hóa LPD)
- Luồng 3 vai trò: Người tạo → Checker → Người phê duyệt
- Phân quyền cơ bản (ai thấy task nào)
- Case: Đồng ý, Từ chối, Yêu cầu bổ sung (trả về bước trước)
- **Mục tiêu**: Hiểu multi-step approval, role-based task assignment

### Phase 5 — Demo LPD Tín Chấp (80% thực tế)
- Luồng đầy đủ: Khởi tạo → Kiểm soát ĐVKD → Checker → Thẩm định → KS Thẩm định → Phê duyệt
- Tất cả case chính: bổ sung, hủy, đồng ý, từ chối
- UI giống hệ thống thật
- **Mục tiêu**: Tái hiện 80% luồng production

---

## Luồng nghiệp vụ LPD Tín Chấp (tham chiếu cho Phase 5)

### Các bước chính
```
1. Khởi tạo ĐXKV (RM) → Đệ trình
2. Kiểm soát ĐVKD → Đệ trình / Yêu cầu bổ sung / Hủy
3. Checker → Đệ trình / Yêu cầu bổ sung
4. Thẩm định ĐXKV → Đệ trình / Yêu cầu bổ sung / Hủy
5. Kiểm soát thẩm định → Đệ trình / Hủy
6. Phê duyệt ĐXKV → Đồng ý / Từ chối / Yêu cầu bổ sung
```

### Transaction Codes
| Code | Tên |
|------|-----|
| STP01 | Khởi tạo |
| STP10 | Kiểm soát |
| STP11 | Checker |
| STP12 | Thẩm định |
| STP13 | KS Thẩm định |
| STP14 | Phê duyệt |
| STP02 | Hủy |
| STP04 | Đồng ý |
| STP05 | Từ chối |

### Vai trò
- RM, Kiểm soát ĐVKD, Checker, Thẩm định viên, KS Thẩm định, Cấp phê duyệt, Admin
