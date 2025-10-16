package com.example.worktime.service;

import com.example.worktime.model.OperationLog;
import com.example.worktime.repository.OperationLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;

@Service
public class OperationLogService {
    private final OperationLogRepository repository;

    public OperationLogService(OperationLogRepository repository) {
        this.repository = repository;
    }

    public void log(String username, String action, String details) {
        if (username == null) return;
        OperationLog log = new OperationLog();
        log.setUsername(username);
        log.setAction(action);
        log.setDetails(details);
        log.setTimestamp(LocalDateTime.now());
        repository.save(log);
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs != null) {
            HttpServletRequest req = attrs.getRequest();
            if (req != null) req.setAttribute("operationLogged", true);
        }
    }
}