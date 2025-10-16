package com.example.worktime.service;

import com.example.worktime.model.Worker;
import com.example.worktime.repository.WorkerRepository;
import org.springframework.stereotype.Service;

/**
 * Service to resolve worker information by code.
 */
@Service
public class WorkerService {

    private final WorkerRepository repository;

    public WorkerService(WorkerRepository repository) {
        this.repository = repository;
    }

    public Worker getByCode(String code) {
        return repository.findByCode(code);
    }
}
