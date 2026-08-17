# Debug trên Camunda Operate — Hướng dẫn chi tiết

---

## Operate là gì?

**Câu chuyện thực tế:**
Camera giám sát trong nhà máy. Bạn không cần đứng cạnh dây chuyền để biết sản phẩm đang ở khâu nào — nhìn màn hình camera là biết.

Operate = camera giám sát cho process. Nó cho bạn thấy:
- Mỗi instance đang ở bước nào
- Variables tại từng bước
- Lỗi xảy ra ở đâu, lỗi gì
- Timeline từng bước đã chạy qua

---

## Truy cập Operate

Cách 1: Từ Camunda Console → cluster → click **"Launch"** cạnh Operate
Cách 2: URL trực tiếp: `https://sin-2.operate.camunda.io/<cluster-id>`

---

## Giao diện chính

```
┌────────────────────────────────────────────────────────────────┐
│  Operate                                                       │
├──────────┬─────────────────────────────────────────────────────┤
│ FILTER   │  BPMN DIAGRAM + thống kê                           │
│          │                                                     │
│ Process  │  (Start) ──▶ [say_hello] ──▶ (End)                 │
│ Version  │                               ② ← 2 completed     │
│          │                                                     │
│ States:  │─────────────────────────────────────────────────────│
│ □ Active │  PROCESS INSTANCES LIST                             │
│ □ Incident│  (hiện khi tick filter)                            │
│ □ Complete│                                                    │
│ □ Canceled│                                                    │
└──────────┴─────────────────────────────────────────────────────┘
```

---

## Bước 1: Lọc instances theo trạng thái

Bên trái có **Instances States**. Tick checkbox để lọc:

| State | Ý nghĩa | Khi nào dùng |
|-------|---------|--------------|
| **Active** | Instance đang chạy, token đang ở giữa flow | Debug "hồ sơ đang kẹt ở đâu?" |
| **Incidents** | Instance bị lỗi, đóng băng | Debug "lỗi gì? ở bước nào?" |
| **Completed** | Instance đã chạy xong End Event | Xem lịch sử, verify kết quả |
| **Canceled** | Instance bị hủy (bởi người hoặc API) | Xem ai hủy, khi nào |

> **Mẹo:** Tick "Active" khi muốn xem realtime. Tick "Completed" khi muốn xem lịch sử.

---

## Bước 2: Xem chi tiết 1 instance

Sau khi tick filter → danh sách instances hiện bên dưới diagram.

Click vào **1 instance** (hoặc click instance key) → vào trang chi tiết:

```
┌─────────────────────────────────────────────────────┐
│ Instance #2251799813685261                           │
├─────────────────────────────────────────────────────┤
│                                                     │
│ DIAGRAM: token highlight (bước nào đã qua)          │
│                                                     │
│ (Start)✓ ──▶ [say_hello]✓ ──▶ (End)✓              │
│                                                     │
├─────────────────────────────────────────────────────┤
│ Flow Node Instances (timeline)                      │
│                                                     │
│ #  │ Element       │ Type          │ Start    │ End │
│ 1  │ Start Event   │ START_EVENT   │ 11:00:01 │ 01 │
│ 2  │ say_hello     │ SERVICE_TASK  │ 11:00:01 │ 02 │
│ 3  │ End Event     │ END_EVENT     │ 11:00:02 │ 02 │
│                                                     │
├─────────────────────────────────────────────────────┤
│ Variables                                           │
│                                                     │
│ name         │ "Thien"                      │ Scope │
│ greeting     │ "Xin chào Thien!"            │ Scope │
│ processedAt  │ 1723878042000                 │ Scope │
└─────────────────────────────────────────────────────┘
```

### Giải thích:

- **Diagram highlight:** Bước nào đã completed → viền xanh/xám. Bước nào token đang đậu → viền xanh đậm + animation.
- **Flow Node Instances:** Timeline — bước nào chạy lúc nào, mất bao lâu. Thấy ngay bottleneck.
- **Variables:** Tất cả biến hiện có trong instance. Click vào Service Task → thấy variables tại scope đó.

---

## Bước 3: Debug Instance đang Active (token đang chạy)

Khi token đang đậu ở 1 task (ví dụ Worker chưa complete):

1. Tick **"Active"** → thấy instance
2. Click vào → diagram highlight task đang chờ
3. Xem variables tại thời điểm đó
4. Biết được: "À, hồ sơ này đang kẹt ở bước Checker vì Worker chưa xử lý"

**Ứng dụng thực tế:**
> "Quản lý hỏi: có bao nhiêu hồ sơ đang kẹt ở Thẩm định?"
> → Operate: tick Active → nhìn số trên diagram → biết ngay.

---

## Bước 4: Debug Incident (khi có lỗi)

Khi Worker throw exception hoặc timeout:

1. Tick **"Incidents"** → thấy instance bị lỗi (icon đỏ)
2. Click vào → diagram highlight bước bị lỗi (viền đỏ)
3. Bên dưới hiện **Incident detail:**
   - Error type: `JOB_NO_RETRIES`
   - Error message: `RuntimeException: Lỗi giả lập...`
   - Retries remaining: 0
4. Bạn biết: lỗi ở Worker `say-hello`, exception message là gì

### Sau khi fix code:

1. Deploy Worker mới (restart Spring Boot)
2. Quay lại Operate → click instance lỗi
3. Click **"Retry"** (nút ở góc) → Zeebe gửi lại job cho Worker
4. Worker mới xử lý thành công → instance tiếp tục

---

## Bước 5: Xem Variables chi tiết

### Xem tất cả variables:
Click instance → tab/panel **"Variables"** bên dưới

### Xem variables tại 1 bước cụ thể:
Click vào **Service Task trên diagram** → Variables panel chỉ hiện variables tại scope đó

### Phân biệt Input vs Output:
- **Trước khi task chạy:** `name = "Thien"` (input, truyền từ start)
- **Sau khi Worker complete:** `greeting = "Xin chào Thien!"` (output, Worker trả về)

### Sửa variable trực tiếp (khi debug):
Click icon **bút chì** cạnh variable → sửa giá trị → Save
→ Dùng khi cần test "nếu biến này khác thì flow đi đâu?"

---

## Bước 6: Kiểm tra sequence flow (token đi đường nào)

Khi có Gateway, muốn biết token rẽ nhánh nào:

1. Click instance có Gateway
2. Nhìn diagram → đường nào được highlight = token đã đi
3. Nhìn variables → xem condition nào đã đúng (ví dụ `approved = true`)

---

## Tóm tắt: Khi nào dùng gì?

| Tình huống | Làm gì trong Operate |
|------------|---------------------|
| "Instance đang ở đâu?" | Tick Active → xem diagram highlight |
| "Lỗi gì?" | Tick Incidents → xem error message |
| "Biến đang có giá trị gì?" | Click instance → Variables panel |
| "Mất bao lâu ở mỗi bước?" | Click instance → Flow Node Instances (timeline) |
| "Token đi nhánh nào?" | Click instance → xem sequence flow highlight |
| "Fix xong, chạy lại" | Click Retry trên instance bị Incident |
| "Hủy instance sai" | Click Cancel trên instance Active |

---

## Thực hành ngay

1. Mở Operate → tick **"Completed"** → xem 2 instances đã chạy
2. Click vào 1 instance → xem Variables → thấy `name`, `greeting`
3. Xem Flow Node Instances → thấy timeline Start → say_hello → End

Muốn thấy Incident thật → sửa Worker throw exception khi `name = "error"` → Postman gửi `{"name": "error"}` → quay lại Operate tick Incidents → thấy lỗi live.
