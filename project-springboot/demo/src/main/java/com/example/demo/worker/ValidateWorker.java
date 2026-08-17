package com.example.demo.worker;

import io.camunda.zeebe.spring.client.annotation.JobWorker;
import io.camunda.zeebe.spring.client.annotation.Variable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class ValidateWorker {

    private static final Logger log = LoggerFactory.getLogger(ValidateWorker.class);

    @JobWorker(type = "auto-validate")
    public Map<String, Object> validate(@Variable String name) {
        log.info(">>> [ValidateWorker] Đang validate: {}", name);

        // Giả lập logic validate
        boolean isValid = name != null && !name.isBlank();

        log.info(">>> [ValidateWorker] Kết quả: valid={}", isValid);
        return Map.of("valid", isValid);
    }
}
