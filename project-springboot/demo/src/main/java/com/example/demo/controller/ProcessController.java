package com.example.demo.controller;

// === Camunda SDK ===
// ZeebeClient - đối tượng giao tiếp với Zeebe Engine (tạo instance, deploy, complete task...)
import io.camunda.zeebe.client.ZeebeClient;
// ProcessInstanceEvent - kết quả trả về sau khi tạo process instance
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

    // Spring tự inject ZeebeClient vào (đã được Camunda SDK tạo sẵn khi app khởi động)
    @Autowired
    private ZeebeClient zeebeClient;

    /**
     * POST /api/v1/process/start
     * Body: { "name": "Nguyen Van A" }
     *
     * Tạo 1 process instance mới → token bắt đầu chạy trong BPMN
     */
    @PostMapping("/start")
    public ResponseEntity<Map<String, Object>> startProcess(@RequestBody Map<String, String> body) {

        String name = body.getOrDefault("name", "World");

        // Gọi Zeebe: tạo instance của process "hello-camunda-process" (ID trong BPMN)
        ProcessInstanceEvent instance = zeebeClient
                .newCreateInstanceCommand()
                .bpmnProcessId("hello-camunda-process")  // phải khớp ID bạn đặt trong Web Modeler
                .latestVersion()                          // dùng version mới nhất đã deploy
                .variables(Map.of("name", name))          // truyền variable "name" vào process
                .send()
                .join();                                   // đợi kết quả (blocking)

        // Trả về thông tin instance vừa tạo
        return ResponseEntity.ok(Map.of(
                "message", "Process đã khởi tạo!",
                "processInstanceKey", instance.getProcessInstanceKey(),
                "bpmnProcessId", instance.getBpmnProcessId()
        ));
    }

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
     * POST /api/v1/process/tasks/{taskKey}/complete
     * Body: { "approved": true }
     *
     * Complete User Task — giả lập người bấm "Phê duyệt"
     */
    @PostMapping("/tasks/{taskKey}/complete")
    public ResponseEntity<Map<String, Object>> completeTask(
            @PathVariable long taskKey,
            @RequestBody Map<String, Object> variables) {

        zeebeClient
                .newUserTaskCompleteCommand(taskKey)
                .variables(variables)
                .send()
                .join();

        return ResponseEntity.ok(Map.of(
                "message", "Task đã complete!",
                "taskKey", taskKey
        ));
    }

}
