package com.example.demo.controller;

// === Camunda SDK ===
import io.camunda.zeebe.client.ZeebeClient;
import io.camunda.zeebe.client.api.response.ProcessInstanceEvent;

// === Spring ===
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// === Java standard ===
import java.util.Map;

@RestController
@RequestMapping("/api/v1/process")
public class ProcessController {

    @Autowired
    private ZeebeClient zeebeClient;

    /**
     * Phase 1: Start process đơn giản (Service Task only)
     * BPMN: Start → [say_hello] → End
     *
     * Mục đích: Tạo 1 process instance → token chạy → Worker tự xử lý → End
     */
    @PostMapping("/start")
    public ResponseEntity<Map<String, Object>> startProcess(@RequestBody Map<String, String> body) {
        String name = body.getOrDefault("name", "World");

        ProcessInstanceEvent instance = zeebeClient
                // newCreateInstanceCommand() — Gửi lệnh lên Zeebe: "tạo 1 instance mới"
                .newCreateInstanceCommand()
                // bpmnProcessId() — Chỉ định chạy process nào (khớp ID trong BPMN đã deploy)
                .bpmnProcessId("hello-camunda-process")
                // latestVersion() — Dùng version mới nhất (nếu deploy nhiều lần, luôn chạy bản mới)
                .latestVersion()
                // variables() — Truyền dữ liệu đầu vào cho process (Worker sẽ đọc được)
                .variables(Map.of("name", name))
                // send() — Gửi request lên Zeebe qua gRPC
                .send()
                // join() — Đợi response trả về (blocking). Không join() → async, phải handle Future
                .join();

        return ResponseEntity.ok(Map.of(
                "message", "Process đã khởi tạo!",
                "processInstanceKey", instance.getProcessInstanceKey(),
                "bpmnProcessId", instance.getBpmnProcessId()
        ));
    }

    /**
     * Phase 2: Start process có User Task
     * BPMN: Start → [Auto Validate] → [Phê duyệt 👤] → End
     *
     * Mục đích: Tạo instance → Worker validate tự động → token DỪNG ở User Task → chờ complete
     */
    @PostMapping("/start-approval")
    public ResponseEntity<Map<String, Object>> startApproval(@RequestBody Map<String, String> body) {
        String name = body.getOrDefault("name", "World");

        ProcessInstanceEvent instance = zeebeClient
                .newCreateInstanceCommand()
                .bpmnProcessId("approval-process")
                .latestVersion()
                .variables(Map.of("name", name))
                .send()
                .join();

        return ResponseEntity.ok(Map.of(
                "message", "Approval process đã khởi tạo!",
                "processInstanceKey", instance.getProcessInstanceKey()
        ));
    }

    /**
     * Complete User Task bằng API
     *
     * Mục đích: Giả lập người dùng bấm "Hoàn thành" trên UI
     * Khi gọi → token tại User Task đó sẽ đi tiếp trong BPMN
     *
     * Lưu ý: SDK 8.5 có thể gặp lỗi SSL với lệnh này → dùng Tasklist UI thay thế
     */
    @PostMapping("/tasks/{taskKey}/complete")
    public ResponseEntity<Map<String, Object>> completeTask(
            @PathVariable long taskKey,
            @RequestBody Map<String, Object> variables) {

        zeebeClient
                // newUserTaskCompleteCommand() — Gửi lệnh "complete User Task" lên Zeebe
                // Khác với newCompleteCommand() (dùng cho Service Task/Job)
                .newUserTaskCompleteCommand(taskKey)
                // variables() — Truyền thêm data khi complete (vd: approved=true, decision="PASS")
                // Merge vào instance, Gateway sẽ đọc để rẽ nhánh
                .variables(variables)
                .send()
                .join();

        return ResponseEntity.ok(Map.of(
                "message", "Task đã complete!",
                "taskKey", taskKey
        ));
    }

    /**
     * Phase 4: Start process multi-role (RM → Checker → Gateway → Phê duyệt)
     * BPMN: Start → [RM 👤] → [Checker 👤] → <XOR> → [Phê duyệt 👤] / End / quay lại RM
     *
     * Mục đích: Test luồng nhiều bước, nhiều vai trò, có case "yêu cầu bổ sung" (RETURN)
     * Variable `decision` quyết định Gateway rẽ nhánh: "PASS" / "REJECT" / "RETURN"
     */
    @PostMapping("/start-multi-role")
    public ResponseEntity<Map<String, Object>> startMultiRole(@RequestBody Map<String, String> body) {
        ProcessInstanceEvent instance = zeebeClient
                .newCreateInstanceCommand()
                .bpmnProcessId("multi-role-process")
                .latestVersion()
                .variables(Map.of("name", body.getOrDefault("name", "World")))
                .send()
                .join();

        return ResponseEntity.ok(Map.of(
                "message", "Multi-role process đã khởi tạo!",
                "processInstanceKey", instance.getProcessInstanceKey()
        ));
    }
}
