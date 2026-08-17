# Phase 1 — Tổng kết kiến thức

---

## Bạn đã làm gì?

```
[Postman] ──POST──▶ [Spring Boot] ──gRPC──▶ [Zeebe SaaS] ──job──▶ [Spring Boot Worker]
                                                                          │
                                                                    complete job
                                                                          │
                                                                          ▼
                                                              [Operate: COMPLETED]
```

Một vòng tròn khép kín — từ request đến kết quả, qua 3 hệ thống.

---

## 6 việc đã làm, mỗi việc dạy 1 khái niệm

| # | Việc đã làm | Khái niệm Camunda học được |
|---|-------------|---------------------------|
| 1 | Tạo Camunda account + cluster | Zeebe Engine chạy trên cloud, Spring Boot là client |
| 2 | Tạo API Client (credentials) | Giao tiếp qua gRPC + OAuth2, không phải REST đơn giản |
| 3 | Vẽ BPMN trên Web Modeler + Deploy | Process Definition = đồ thị bất biến, deploy lên Zeebe |
| 4 | Code `ProcessController` + gọi Postman | Start instance = tạo 1 token bắt đầu chạy qua diagram |
| 5 | Code `HelloWorker` với `@JobWorker` | Worker = nhân viên chờ việc, lắng nghe job theo type |
| 6 | Xem Operate (Completed + Incidents) | Giám sát = xem token đi đâu, variables là gì, lỗi ở đâu |

---

## Khái niệm đã nắm

### Process Definition vs Process Instance
- BPMN file = khuôn mẫu (không đổi)
- Mỗi lần gọi `newCreateInstanceCommand()` = 1 instance mới chạy trên khuôn đó

### Token
- Con trỏ di chuyển qua từng node trong BPMN
- Đậu tại task → đợi complete → đi tiếp
- Đến End Event → biến mất → instance COMPLETED

### Service Task + Job + Worker
- Token đến Service Task → Zeebe tạo Job vào queue
- Worker (long polling) kéo job về → xử lý → complete
- Worker không expose endpoint, chủ động hỏi Zeebe

### Variables
- Map key-value gắn với instance
- Input: truyền khi start (`name = "Thien"`)
- Output: Worker trả về (`greeting = "Xin chào Thien!"`)
- Merge — không replace

### Incident
- Worker throw exception + hết retry → Incident
- Instance đóng băng, không mất
- Fix code → Retry → instance tiếp tục

---

## Code mapping

```java
// === Start Process ===
zeebeClient
    .newCreateInstanceCommand()
    .bpmnProcessId("hello-camunda-process")  // ID trong BPMN
    .latestVersion()
    .variables(Map.of("name", name))          // input variables
    .send().join();

// === Worker xử lý ===
@JobWorker(type = "say-hello")               // khớp Task Definition Type trong BPMN
public Map<String, Object> handle(@Variable String name) {
    // logic...
    return Map.of("greeting", "...");        // output variables, tự complete
}
```

---

## BPMN đã vẽ

```
(Start) ──▶ [say_hello] ──▶ (End)
              type: "say-hello"
```

- 3 elements: Start Event, Service Task, End Event
- Task Definition Type = `say-hello` → map với `@JobWorker(type = "say-hello")`

---

## Operate đã dùng

| Filter | Dùng khi |
|--------|---------|
| Active | Xem instance đang chạy (token đậu ở đâu) |
| Incidents | Xem instance bị lỗi (error message, bước nào) |
| Completed | Xem instance đã xong (verify kết quả, variables) |
| Canceled | Xem instance bị hủy |

Click instance → thấy: diagram highlight + variables + timeline.

---

## Câu hỏi tự kiểm tra

1. **Worker nhận job bằng cách nào?** → Long polling — chủ động hỏi Zeebe, không phải Zeebe push
2. **Nếu restart Spring Boot, instance có mất không?** → Không. Zeebe lưu state. Worker khi start lại sẽ poll lại job.
3. **2 Worker cùng type chạy song song thì sao?** → Zeebe tự load balance, mỗi job chỉ giao cho 1 Worker.
4. **Variables Worker trả về đi đâu?** → Merge vào instance, bước tiếp theo đọc được.
5. **Incident xảy ra khi nào?** → Worker throw exception + hết retry (default 3 lần).

---

## Phase 2 sẽ thêm gì?

```
(Start) ──▶ [Service Task] ──▶ [User Task: chờ người bấm] ──▶ (End)
```

Khác biệt chính: **User Task dừng process**, đợi con người gọi API complete.
Đây là nền tảng cho luồng phê duyệt — Checker, Thẩm định, Phê duyệt đều là User Task.
