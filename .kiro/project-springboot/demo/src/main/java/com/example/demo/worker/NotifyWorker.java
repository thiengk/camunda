package com.example.demo.worker;

import io.camunda.zeebe.spring.client.annotation.JobWorker;
import io.camunda.zeebe.spring.client.annotation.Variable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class NotifyWorker {

    private static final Logger log = LoggerFactory.getLogger(NotifyWorker.class);

    @JobWorker(type = "notify-approved")
    public void handleApproved(@Variable String name) {
        log.info(">>> [APPROVED] Hồ sơ của {} đã được PHÊ DUYỆT!", name);
    }

    @JobWorker(type = "notify-rejected")
    public void handleRejected(@Variable String name) {
        log.info(">>> [REJECTED] Hồ sơ của {} đã bị TỪ CHỐI!", name);
    }
}
