package com.example.worktime.controller;

import com.example.worktime.model.OperationLog;
import com.example.worktime.repository.OperationLogRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/logs")
@CrossOrigin
public class OperationLogController {
    private final OperationLogRepository repository;

    public OperationLogController(OperationLogRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<OperationLog> logs() {
        return repository.findAllByOrderByTimestampDesc();
    }
}