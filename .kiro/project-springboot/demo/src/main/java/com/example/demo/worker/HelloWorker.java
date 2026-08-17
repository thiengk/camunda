package com.example.demo.worker;
// @JobWorker - đánh dấu hàm này là Worker, lắng nghe job từ Zeebe
import io.camunda.zeebe.spring.client.annotation.JobWorker;
// @Variable - tự động map variable từ process instance vào tham số hàm
import io.camunda.zeebe.spring.client.annotation.Variable;
// Logger - ghi log ra console (thay cho System.out.println)
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import java.util.Map;
@Component
public class HelloWorker {
    // muc dich de lam gi ?
    private static final Logger log = LoggerFactory.getLogger(HelloWorker.class);
    // "say-hello" phải khớp với Task Definition Type trong BPMN
//    @JobWorker(type = "say-hello")
//    public Map<String, Object> handleSayHello(@Variable String name) {
//        // Logic xử lý — in ra tên
//        log.info(">>> Worker nhận job - Xin chào: {}", name);
//
//        // Trả về variables mới — merge vào process instance
//        return Map.of(
//                "greeting", "Xin chào " + name + "!",
//                "processedAt", System.currentTimeMillis()
//        );
//    }
    @JobWorker(type = "say-hello")
    public Map<String, Object> handleSayHello(@Variable String name) {
        if ("error".equals(name)) {
            throw new RuntimeException("Lỗi giả lập: name không hợp lệ!");
        }
        log.info(">>> Worker nhận job - Xin chào: {}", name);
        return Map.of("greeting", "Xin chào " + name + "!", "processedAt", System.currentTimeMillis());
    }

}
