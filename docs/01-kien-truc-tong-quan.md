# Kiến trúc tổng quan Camunda 8

> Đọc file này với tư duy: bạn đã biết React và Spring Boot.
> Camunda là mảnh ghép thứ 3. Câu hỏi cần trả lời:
> **"Khi request từ FE xuống BE, Camunda nằm ở đâu và làm gì?"**

---

## Bức tranh toàn cảnh

```
┌──────────┐      HTTP/REST      ┌─────────────────┐      gRPC       ┌──────────────────────┐
│  React   │ ─────────────────▶ │   Spring Boot   │ ─────────────▶ │   Camunda (Zeebe)    │
│  (FE)    │ ◀───────────────── │   (BE)          │ ◀───────────── │   (Process Engine)   │
└──────────┘                    └─────────────────┘                └──────────────────────┘
```

Nhìn vào đây, Camunda **không phải** là một phần của Spring Boot.
Nó là một **hệ thống riêng biệt**, giống như khi bạn dùng Redis hay Kafka —
Spring Boot chỉ là client kết nối vào nó.

---

## Camunda gồm những gì bên trong?

```
┌─────────────────────────────────────────────────────────────────────┐
│                        CAMUNDA 8 Platform                           │
│                                                                     │
│  ┌─────────────┐   ┌──────────────┐   ┌───────────┐   ┌─────────┐   │
│  │Web Modeler  │   │    Zeebe     │   │  Operate  │   │Tasklist │   │
│  │(vẽ BPMN)   │   │  (Engine)    │   │(giám sát) │   │(UI task)│    │
│  └─────────────┘   └──────┬───────┘   └───────────┘   └─────────┘   │
│                           │                                         │
│                    ┌──────┴────────┐                                │
│                    │  Job Queue    │  ← trái tim của engine         │
│                    │  Event Log    │  ← bộ nhớ của engine           │
│                    └───────────────┘                                │
└─────────────────────────────────────────────────────────────────────┘
```

4 thành phần, mỗi thành phần một nhiệm vụ rõ ràng:

| Thành phần | Là gì trong đời thực | Làm gì |
|---|---|---|
| **Web Modeler** | Bảng vẽ sơ đồ quy trình | Vẽ BPMN bằng kéo thả, deploy lên Zeebe |
| **Zeebe** | Bộ não điều phối | Chạy process, quản lý trạng thái, điều phối task |
| **Operate** | Bảng giám sát CCTV | Xem process đang chạy ở đâu, bị kẹt ở đâu |
| **Tasklist** | Hòm thư công việc | UI mặc định để con người nhận và làm task |

> **Trong thực tế:** Ta tự build UI bằng React thay cho Tasklist.
> Operate vẫn dùng để debug và giám sát.

---

## Zeebe — Bộ não, đi sâu hơn

Zeebe không phải database, không phải message queue đơn thuần.
Nó là **distributed workflow engine** — hiểu nôm na:

> "Một hệ thống biết mọi đơn hàng đang ở bước nào,
> tự động phân công việc đúng người, đúng lúc,
> và không bao giờ quên dù có sự cố."

Bên trong Zeebe có 2 thứ quan trọng nhất:

```
┌─────────────────────────────────────┐
│              ZEEBE                  │
│                                     │
│  ┌─────────────┐  ┌──────────────┐  │
│  │  Job Queue  │  │  Event Log   │  │
│  │             │  │              │  │
│  │ [job A    ] │  │ CREATED      │  │
│  │ [job B    ] │  │ ACTIVATED    │  │
│  │ [job C    ] │  │ COMPLETED    │  │
│  └─────────────┘  │ COMPLETED    │  │
│                   └──────────────┘  │
└─────────────────────────────────────┘
```

**Job Queue** — hàng đợi công việc cần được xử lý  
**Event Log** — nhật ký mọi sự kiện đã xảy ra (append-only, không xóa)

---

## Khái niệm 1: Process Definition vs Process Instance

**Câu chuyện thực tế:**
Tờ đơn xin việc là một mẫu in sẵn (Process Definition).
Mỗi người điền vào tờ đó là một hồ sơ riêng (Process Instance).
1000 người nộp đơn = 1000 hồ sơ đang chạy song song, mỗi cái ở bước khác nhau.

```
Process Definition (BPMN file — bất biến):
[Nộp đơn] → [Kiểm tra] → [Phỏng vấn] → [Phê duyệt] → [Kết thúc]

Process Instance 001: đang ở [Kiểm tra]
Process Instance 002: đang ở [Phỏng vấn]
Process Instance 003: đang ở [Phê duyệt]
Process Instance 004: mới tạo, ở [Nộp đơn]
```

**Về mặt kỹ thuật:**
- Definition: file BPMN được deploy lên Zeebe, có `processDefinitionKey`
- Instance: một lần chạy cụ thể, có `processInstanceKey`, có variables riêng
- Thay đổi BPMN → deploy version mới → instance cũ vẫn chạy version cũ

---

## Khái niệm 2: Token

**Câu chuyện thực tế:**
Số thứ tự ở bệnh viện. Bạn lấy số A045.
Tờ giấy đó theo bạn qua từng phòng: đăng ký → xét nghiệm → bác sĩ → thu ngân.
Mỗi phòng xem số, làm xong, cho bạn đi tiếp.

**Token = tờ số thứ tự của một process instance.**

```
BPMN:
[Start] ──▶ [Task A] ──▶ [Task B] ──▶ [End]

Token chạy:
● ──────────▶ ●(đậu tại Task A, đợi xử lý)
                ──────────▶ ●(đậu tại Task B)
                              ──────────▶ ● (End, biến mất)
```

**Parallel Gateway — Token nhân bản:**
```
                        ┌──▶ [Task B] ──▶ ┐
[Task A] ──▶ [AND Split]│                 │[AND Join]──▶ [Task D]
                        └──▶ [Task C] ──▶ ┘

Token A vào → nhân thành Token B và Token C
Cả 2 chạy song song
AND Join đợi cả 2 token đến mới tạo Token D tiếp tục
```

---

## Khái niệm 3: Service Task vs User Task

**Câu chuyện thực tế:**

Dây chuyền đóng gói ở nhà máy:
- **Máy dán nhãn** (Service Task) — tự động, không cần người, chạy liên tục
- **Kiểm tra chất lượng** (User Task) — người thật phải cầm sản phẩm lên xem, bấm nút OK

```
SERVICE TASK                        USER TASK
─────────────────────────────────   ────────────────────────────────
Zeebe tạo JOB vào queue             Zeebe tạo TASK trong Tasklist
Worker (code Java) tự xử lý         Con người xử lý qua UI
Xong trong milliseconds             Xong trong phút/giờ/ngày
Process tiếp tục ngay               Process DỪNG, chờ người bấm
```

Nhưng về mặt engine, cả hai đều làm **1 việc duy nhất**:

> Token đậu tại đây → đợi ai đó gửi "complete" → token đi tiếp.

Chỉ khác nhau ở **ai** gửi "complete":
- Service Task: Worker (code) gửi
- User Task: Con người bấm UI gửi

---

## Khái niệm 4: Job và Worker

**Câu chuyện thực tế:**
Bưu điện trung tâm nhận kiện hàng → đặt vào kệ phân loại theo khu vực.
Nhân viên giao hàng từng khu đến lấy phần của mình khi họ sẵn sàng → giao xong → báo lại bưu điện.

- Bưu điện không đến tận nhà nhân viên để giao việc
- Nhân viên chủ động ra lấy (pull, không phải push)
- Thêm nhân viên = giao hàng nhanh hơn, không cần thay đổi bưu điện

```
Zeebe (Bưu điện)              Worker trong Spring Boot (Nhân viên)
──────────────────            ──────────────────────────────────────
Tạo Job "send-email" ──┐      @JobWorker(type = "send-email")
Tạo Job "send-email" ──┤      public void handle(job) {
Tạo Job "send-email" ──┘          // xử lý logic
       ↑                          client.completeJob(job.getKey());
       │                      }
       └── Worker poll ───────────────────────────────────────────▶
           Worker lấy job ◀───────────────────────────────────────
           Worker complete ───────────────────────────────────────▶
```

**Worker hoạt động bằng Long Polling:**

```
Worker: "Zeebe ơi, có job type=send-email cho tôi không?"
Zeebe:  "Chưa, đợi tôi tí..." (giữ connection 30 giây)
Zeebe:  "Có rồi! Đây job của mày."  ← trả về ngay khi có
Worker: xử lý → complete
Worker: "Zeebe ơi, có job type=send-email cho tôi không?" (poll lại)
```

> Worker KHÔNG expose endpoint. Nó chủ động hỏi Zeebe.
> Zeebe KHÔNG biết Worker đang ở đâu, bao nhiêu instances.
> Scale Worker = thêm pod/instance, không cần config gì thêm.

---

## Khái niệm 5: Process Variables

**Câu chuyện thực tế:**
Hồ sơ vay ngân hàng. Từ khi nộp đến khi giải ngân đi qua 8 người.
Mỗi người đọc thông tin cũ và ghi thêm nhận xét của mình.
Hồ sơ ngày càng dày hơn, không ai xóa thông tin của người trước.

```
Start: variables = { loanAmount: 500_000_000, applicantId: "A001" }

After Checker:   + { checkerNote: "Hồ sơ đủ điều kiện", checkerPass: true }

After Thẩm định: + { creditScore: 720, collateralValue: 800_000_000 }

After Phê duyệt: + { approved: true, approvedBy: "Manager_01", approvedAt: "..." }

Gateway đọc: approved == true → đi nhánh giải ngân
```

Variables được **merge** — không replace.
Gateway dùng variables để quyết định đi nhánh nào (FEEL expression).

---

## Khái niệm 6: Gateway

**Câu chuyện thực tế:**
Ngã tư giao thông. Đèn và biển báo quyết định xe đi hướng nào.
Xe không tự quyết — luật giao thông quyết định dựa trên điều kiện.

```
EXCLUSIVE GATEWAY (XOR) — chọn 1 trong nhiều:
            ┌──▶ [Giải ngân]      (nếu approved = true)
[Phê duyệt]─┤
            └──▶ [Từ chối]        (nếu approved = false)

PARALLEL GATEWAY (AND) — chạy tất cả cùng lúc:
            ┌──▶ [Gửi email]
[Phê duyệt]─┤
            └──▶ [Cập nhật core banking]
            (2 việc chạy song song, không chờ nhau)

INCLUSIVE GATEWAY (OR) — chạy những nhánh thỏa điều kiện:
            ┌──▶ [Notify SMS]    (nếu có số điện thoại)
[Phê duyệt]─┤
            └──▶ [Notify Email]  (nếu có email)
            (có thể 1 hoặc cả 2)
```

**Không cần code Java.** Zeebe tự evaluate expression từ variables.

---

## Khái niệm 7: Incident

**Câu chuyện thực tế:**
Dây chuyền sản xuất ô tô. Robot hàn đột nhiên hỏng.
Dây chuyền KHÔNG bỏ qua bước hàn — chiếc xe đó **dừng lại tại chỗ**.
Đèn đỏ bật. Kỹ sư đến sửa. Xong → chiếc xe tiếp tục.
Không ai sản xuất ra xe thiếu mối hàn.

```
Worker throw Exception
        │
        ▼
Zeebe retry (nếu retries > 0)
        │
        ▼ (hết retry)
INCIDENT được tạo
        │
        ├── Process instance ĐÓNG BĂNG (không cancel, không mất data)
        ├── Hiển thị trong Camunda Operate
        │
        ▼
Developer xem log, fix bug, deploy Worker mới
        │
        ▼
Resolve incident trong Operate
        │
        ▼
Process tiếp tục từ đúng chỗ đó
```

> Đây là điểm mạnh nhất: **không bao giờ mất dữ liệu khi có lỗi**.

---

## Luồng hoàn chỉnh: Request từ React → qua Spring Boot → Camunda

### Luồng 1: Tạo mới process (RM nộp hồ sơ)

```
React                   Spring Boot                    Zeebe
  │                          │                           │
  │─POST /api/loan/submit────▶│                           │
  │  body: {loanAmount, ...}  │                           │
  │                          │──newCreateInstance()──────▶│
  │                          │  bpmnProcessId: "lpd"      │── Tạo Process Instance
  │                          │  variables: {loan: ...}    │── Token xuất phát
  │                          │◀──processInstanceKey───────│── Token đậu tại Task đầu
  │◀──{instanceKey: 123}─────│                           │
```

### Luồng 2: Service Task tự động (Worker xử lý)

```
Zeebe                              Spring Boot Worker
  │                                       │
  │ Token đến Service Task "validate"     │
  │── Tạo Job "validate-loan" ───────────▶│ (long poll đang chờ)
  │ Process DỪNG                          │
  │                                       │ @JobWorker(type="validate-loan")
  │                                       │ xử lý logic...
  │◀── completeJob(variables) ────────────│
  │                                       │
  │ Token tiếp tục → sang Task tiếp theo  │
```

### Luồng 3: User Task (Checker phê duyệt)

```
React (Checker)          Spring Boot                  Zeebe
  │                          │                          │
  │                          │           Token đến User Task "Checker Review"
  │                          │           Zeebe tạo Task, DỪNG process
  │                          │                          │
  │─GET /api/tasks───────────▶│                          │
  │                          │──queryUserTasks()─────────▶│
  │◀──[{taskId, name...}]────│◀──[tasks]─────────────────│
  │                          │                          │
  │ Checker xem, bấm "Duyệt" │                          │
  │─POST /api/tasks/{id}/done▶│                          │
  │  body: {decision: "PASS"} │                          │
  │                          │──completeTask(variables)──▶│
  │◀──{success}──────────────│                          │
  │                          │           Token tiếp tục → Gateway
  │                          │           Gateway đọc decision="PASS"
  │                          │           → đi nhánh Thẩm định
```

---

## Mental Model cuối cùng

Nếu phải giải thích Camunda trong 3 câu:

> **Camunda là người quản lý quy trình.**
> Bạn vẽ ra quy trình (BPMN), Camunda nhớ mọi hồ sơ đang ở bước nào.
> Spring Boot chỉ làm việc khi Camunda gọi — không phải Spring Boot điều khiển flow.

```
Không có Camunda:              Có Camunda:
─────────────────              ─────────────────────────────
Spring Boot tự nhớ             Zeebe nhớ thay
  "hồ sơ 001 đang ở            Spring Boot chỉ hỏi:
   bước CHECKER"               "Có việc gì cho tôi không?"

Restart app → mất             Restart app → không mất
1000 hồ sơ → tự query DB     1000 hồ sơ → Zeebe quản lý
Logic flow nằm trong code     Logic flow nằm trong BPMN
```

---

## Ví dụ thực tế: Luồng phê duyệt Tín Chấp — Tại sao không dùng Java thuần?

---

### Luồng nghiệp vụ thật

```
RM khởi tạo ──▶ Checker kiểm tra ──▶ Thẩm định ──▶ Phê duyệt ──▶ Hoàn thành
                      │                   │              │
                      │                   │              │
                      ▼                   ▼              ▼
                Yêu cầu bổ sung    Yêu cầu bổ sung  Yêu cầu bổ sung
                (trả về RM)        (trả về RM)      (trả về Thẩm định)
```

Mỗi bước có thể:
- **Đệ trình** → sang bước tiếp theo
- **Yêu cầu bổ sung** → quay ngược về bước trước
- **Hủy** → kết thúc hồ sơ

---

### Giả sử bạn code bằng Java thuần (không dùng Camunda)

```java
// LoanService.java — "trông có vẻ đơn giản"

public void submitByRM(Long appId, LoanData data) {
    LoanApplication app = repository.findById(appId);
    app.setStatus("CHECKER");
    app.setAssignee(findAvailableChecker());
    repository.save(app);
}

public void submitByChecker(Long appId, String decision) {
    LoanApplication app = repository.findById(appId);
    if (decision.equals("PASS")) {
        app.setStatus("THAM_DINH");
        app.setAssignee(findAvailableAppraiser());
    } else if (decision.equals("RETURN")) {
        app.setStatus("RM");
        app.setAssignee(app.getCreatedBy()); // trả về RM
    }
    repository.save(app);
}

public void submitByAppraiser(Long appId, String decision) {
    LoanApplication app = repository.findById(appId);
    if (decision.equals("PASS")) {
        app.setStatus("PHE_DUYET");
        app.setAssignee(findApprover(app.getLoanAmount()));
    } else if (decision.equals("RETURN")) {
        app.setStatus("RM");
    }
    repository.save(app);
}

public void submitByApprover(Long appId, String decision) {
    // ... và cứ thế thêm if/else...
}
```

**Nhìn sơ qua thì chạy được. Nhưng...**

---

### 10 vấn đề thực tế khi không dùng Camunda

| # | Vấn đề | Chi tiết |
|---|--------|----------|
| 1 | **Flow nằm rải rác** | Logic "đi đâu tiếp theo" nằm trong 10 hàm, 5 file. Người mới vào team đọc không ra toàn bộ flow. |
| 2 | **Trạng thái dễ sai** | `app.setStatus("THAM_DINH")` — viết sai chính tả? Compile vẫn pass. Runtime mới biết. |
| 3 | **Yêu cầu bổ sung = spaghetti** | RM bổ sung xong submit lại → quay lại Checker hay Thẩm định? Phải lưu thêm `previousStep`, `returnTo`... Code ngày càng rối. |
| 4 | **App restart = mất context** | Nếu server restart giữa chừng (deploy mới, crash) — hồ sơ đang ở bước nào? Phải tự query DB, tự restore, tự validate. |
| 5 | **Timeout/SLA** | "Checker phải xử lý trong 4 giờ, quá giờ thì auto escalate" — bạn tự viết scheduler quét DB mỗi phút? Tự handle timezone, ngày nghỉ? |
| 6 | **Visibility** | Quản lý hỏi: "Có bao nhiêu hồ sơ đang kẹt ở Thẩm định?" — bạn viết thêm dashboard query DB, group by status. Rồi "hiển thị flow đang ở đâu bằng hình?" — viết thêm UI. |
| 7 | **Thay đổi flow** | Sếp nói "thêm bước KS Thẩm định giữa Thẩm định và Phê duyệt" — refactor code, migration DB, test regression. Hồ sơ cũ đang ở Thẩm định → có được chuyển thẳng sang bước mới không? |
| 8 | **Retry khi lỗi** | Service call bị timeout → retry? Retry bao nhiêu lần? Khoảng cách giữa các lần? Tự implement? Dead letter queue? |
| 9 | **Audit trail** | "Ai đã làm gì, lúc nào, ở bước nào?" — tự tạo bảng `audit_log`, tự ghi vào mọi chỗ. Quên 1 chỗ = mất dấu vết. |
| 10 | **Parallel / phân cấp phê duyệt** | "Khoản vay > 5 tỷ cần 2 người phê duyệt song song" — code `if amount > 5B then waitForBothApprovals()` → race condition, partial complete... |

---

### Cùng luồng đó, dùng Camunda thì sao?

**BPMN diagram (vẽ kéo thả, không code):**

```
[Start]
   │
   ▼
[RM khởi tạo]  ◀──────────────────────────────────────┐
   │                                                   │
   ▼                                                   │
[Checker kiểm tra] ─── decision="RETURN" ─────────────▶│
   │                                                   │
   │ decision="PASS"                                   │
   ▼                                                   │
[Thẩm định] ────────── decision="RETURN" ─────────────▶│
   │                                                   │
   │ decision="PASS"                                   │
   ▼                                                   │
[Phê duyệt] ────────── decision="RETURN" ─────────────▶│
   │
   │ decision="APPROVE" / "REJECT"
   ▼
[XOR Gateway]
   ├── approved=true  ──▶ [Giải ngân] ──▶ [End: Thành công]
   └── approved=false ──▶ [Thông báo từ chối] ──▶ [End: Từ chối]
```

**Java code cho MỖI bước chỉ là business logic thuần:**

```java
// Không cần biết bước tiếp theo là gì. BPMN lo.
// Không cần set status. Zeebe lo.
// Không cần biết returnTo. BPMN có arrow quay lại.

// CheckerWorker.java — chỉ validate hồ sơ
@JobWorker(type = "checker-auto-validate")
public Map<String, Object> validate(@Variable Long appId) {
    LoanApplication app = repository.findById(appId);
    boolean isValid = checkDocuments(app);
    return Map.of("documentsValid", isValid);
}

// ProcessController.java — complete user task
@PostMapping("/tasks/{taskId}/complete")
public void completeTask(@PathVariable String taskId, @RequestBody Map<String, Object> vars) {
    // vars = { decision: "PASS" } hoặc { decision: "RETURN" }
    zeebeClient.newCompleteCommand(Long.parseLong(taskId))
        .variables(vars)
        .send().join();
    // Xong. Zeebe tự biết token đi đâu dựa trên BPMN.
}
```

---

### So sánh trực tiếp

| Vấn đề | Java thuần | Camunda |
|--------|-----------|---------|
| Flow logic ở đâu? | Rải rác trong code | 1 file BPMN, nhìn là hiểu |
| Yêu cầu bổ sung? | `if/else` + `returnTo` field | Arrow quay lại trong diagram |
| App restart? | Tự handle | Zeebe lưu state bền vững |
| Timeout/SLA? | Tự viết scheduler | Timer Event trong BPMN |
| Visibility? | Tự build dashboard | Operate có sẵn |
| Thay đổi flow? | Refactor code + DB migration | Sửa BPMN, deploy version mới |
| Retry? | Tự implement | Config retries trên task |
| Audit trail? | Tự ghi log | Event Log có sẵn |
| Code mỗi bước? | Biết cả flow + logic | Chỉ biết logic của bước mình |

---

### Tại sao "Yêu cầu bổ sung" cực kỳ khó code thuần?

**Câu chuyện thực tế:**
Bạn nộp hồ sơ xin visa. Cán bộ nói "thiếu giấy tờ, bổ sung đi".
Bạn bổ sung xong nộp lại. Lần này cán bộ **khác** kiểm tra.
Nhưng hồ sơ vẫn là hồ sơ cũ, chỉ thêm giấy tờ mới.

**Với Java thuần:**
```java
if (decision.equals("RETURN")) {
    app.setStatus("RM");
    app.setReturnFrom("CHECKER"); // phải nhớ trả từ đâu
    app.setReturnReason(reason);
    app.setReturnCount(app.getReturnCount() + 1); // đếm bao nhiêu lần
    // RM submit lại → phải check returnFrom để biết gửi về đâu
    // returnFrom = "CHECKER" → gửi lại Checker
    // returnFrom = "THAM_DINH" → gửi lại Thẩm định
    // returnFrom = "PHE_DUYET" → gửi lại Phê duyệt
    // Lỡ bị return 2 lần liên tiếp từ 2 bước khác nhau → ???
}
```

**Với Camunda:**
```
                    decision="RETURN"
[Checker] ─────────────────────────────────▶ [RM khởi tạo]
                                                  │
                                                  │ submit lại
                                                  ▼
                                              [Checker]  ← Token tự quay lại đây
```

Không cần `returnFrom`, không cần `returnCount`, không cần suy nghĩ.
BPMN có arrow quay lại → Token đi theo arrow → Tự động đúng.

---

### Kết luận: Khi nào KHÔNG cần Camunda?

Camunda **overkill** nếu:
- Flow chỉ có 2-3 bước đơn giản, không thay đổi
- Không có User Task (mọi thứ tự động)
- Không cần visibility / audit
- Team nhỏ, product đơn giản

Camunda **cần thiết** khi:
- Flow phức tạp, nhiều bước, nhiều nhánh
- Có User Task (người thật phải thao tác)
- Flow thay đổi thường xuyên theo business
- Cần biết hồ sơ đang ở đâu, kẹt ở đâu
- Cần SLA, timeout, retry có sẵn
- Nhiều team cùng phát triển trên 1 process

> **LPD Tín Chấp = 6 bước, 6 vai trò, 3 loại quyết định, yêu cầu bổ sung, SLA, hội đồng phê duyệt.**
> → Đây chính xác là lúc Camunda tỏa sáng.
