# Phase 2 & 3 — Tổng kết: User Task + Gateway

---

## Đã làm gì?

```
(Start) ──▶ [Auto Validate ⚙️] ──▶ [Phê duyệt 👤] ──▶ <XOR Gateway> ──▶ [Notify Approved ⚙️] ──▶ (End)
                                                              │
                                                              └──▶ [Notify Rejected ⚙️] ──▶ (End)
```

Một process có đủ 3 loại element cốt lõi:
- Service Task (tự động)
- User Task (con người)
- Gateway (rẽ nhánh)

---

## Khái niệm mới học được

### User Task

| Câu hỏi | Trả lời |
|---------|---------|
| Khác Service Task chỗ nào? | Process DỪNG, chờ con người complete |
| Token đi tiếp khi nào? | Khi người dùng bấm Complete (Tasklist hoặc API) |
| Ai thấy task? | Người được assign hoặc trong candidate group |
| Variables truyền vào từ đâu? | Từ process + người dùng thêm khi complete |

**Câu chuyện thực tế:** Hồ sơ nằm trên bàn Checker — đợi Checker đọc xong bấm "Duyệt" hoặc "Trả lại".

---

### Exclusive Gateway (XOR)

| Câu hỏi | Trả lời |
|---------|---------|
| Làm gì? | Chọn **1 trong nhiều** nhánh dựa trên condition |
| Condition viết ở đâu? | Trên mũi tên (sequence flow) ra khỏi Gateway |
| Syntax? | FEEL expression: `= approved = true` |
| Nếu không nhánh nào match? | **Incident** — trừ khi có default flow |
| Ai evaluate condition? | Zeebe Engine — không cần code Java |

**Câu chuyện thực tế:** Ngã tư — đèn xanh chỉ bật 1 hướng dựa trên luồng xe.

---

## Bài học từ lỗi thực tế

### Lỗi 1: Tên variable sai (`approval` vs `approved`)
- BPMN condition check `approved`
- Tasklist nhập `approval`
- Gateway không tìm thấy → Incident

**Bài học:** Tên variable phải khớp chính xác giữa BPMN condition và code/UI.

### Lỗi 2: Type variable sai (string `"false"` vs boolean `false`)
- Condition: `= approved = false` (compare boolean)
- Nhập `"false"` (string) → không match

**Bài học:** FEEL so sánh strict type. `"true"` (string) ≠ `true` (boolean).

### Lỗi 3: Không có default flow
- Không nhánh nào match → Incident
- Fix: set 1 nhánh là default (giống `else`)

**Bài học:** Luôn có default flow hoặc đảm bảo ít nhất 1 condition luôn true.

---

## Code đã viết

```java
// ValidateWorker.java — Service Task tự động
@JobWorker(type = "auto-validate")
public Map<String, Object> validate(@Variable String name) {
    boolean isValid = name != null && !name.isBlank();
    return Map.of("valid", isValid);
}

// NotifyWorker.java — 2 Worker cho 2 nhánh Gateway
@JobWorker(type = "notify-approved")
public void handleApproved(@Variable String name) {
    log.info(">>> [APPROVED] Hồ sơ của {} đã được PHÊ DUYỆT!", name);
}

@JobWorker(type = "notify-rejected")
public void handleRejected(@Variable String name) {
    log.info(">>> [REJECTED] Hồ sơ của {} đã bị TỪ CHỐI!", name);
}

// ProcessController.java — Start approval process
@PostMapping("/start-approval")
public ResponseEntity<Map<String, Object>> startApproval(@RequestBody Map<String, String> body) {
    ProcessInstanceEvent instance = zeebeClient
        .newCreateInstanceCommand()
        .bpmnProcessId("approval-process")
        .latestVersion()
        .variables(Map.of("name", body.getOrDefault("name", "World")))
        .send().join();
    return ResponseEntity.ok(Map.of("processInstanceKey", instance.getProcessInstanceKey()));
}
```

---

## Operate — Debug Gateway

Khi click vào instance completed:
- Diagram highlight cho thấy **token đi nhánh nào** (Approved hoặc Rejected)
- Variables tab cho thấy giá trị `approved` tại thời điểm Gateway evaluate
- Nếu Incident → tab Incidents cho thấy "Condition error" + element nào fail

---

## Map vào nghiệp vụ Tín Chấp

| Phase 2-3 | LPD Tín Chấp thật |
|-----------|-------------------|
| User Task "Phê duyệt" | Checker review / Thẩm định / Phê duyệt |
| `approved = true` | decision = "PASS" → đi bước tiếp |
| `approved = false` | decision = "REJECT" → từ chối |
| Gateway XOR | Rẽ nhánh: Đồng ý / Từ chối / Yêu cầu bổ sung |
| Incident (tên biến sai) | Lỗi config BPMN — cần đồng bộ biến giữa code và diagram |

---

## Câu hỏi tự kiểm tra

1. **User Task khác Service Task chỗ nào?** → Dừng process, chờ người complete. Service Task tự Worker xử lý.
2. **Gateway evaluate condition ở đâu?** → Trong Zeebe Engine, đọc variables của instance, so sánh theo FEEL.
3. **Nếu không nhánh nào match?** → Incident. Fix: thêm default flow hoặc sửa condition/variable.
4. **Có cần code Java cho Gateway không?** → Không. Chỉ cần set condition trên mũi tên trong BPMN.
5. **Variable `approved` đến từ đâu?** → Người dùng thêm khi complete User Task (trên Tasklist hoặc qua API).

---

## Tổng tiến độ

- ✅ Phase 1: Service Task + Worker + Deploy + Operate
- ✅ Phase 2: User Task (con người bấm, process dừng chờ)
- ✅ Phase 3: Gateway (rẽ nhánh theo condition)
- ⬜ Phase 4: Multi-role approval (nhiều bước, nhiều vai trò)
- ⬜ Phase 5: Demo LPD Tín Chấp

---

## Phase 4 sẽ thêm gì?

```
(Start) ──▶ [RM tạo hồ sơ 👤] ──▶ [Checker kiểm tra 👤] ──▶ <XOR> ──▶ [Phê duyệt 👤] ──▶ (End)
                                          │
                                          └── decision="RETURN" ──▶ quay lại [RM]
```

Nhiều User Task liên tiếp, nhiều vai trò, case "yêu cầu bổ sung" (quay ngược).
