package com.example.worktime.controller;

import com.example.worktime.model.Worker;
import com.example.worktime.repository.WorkerRepository;
import org.springframework.http.HttpStatus;
import com.example.worktime.service.OperationLogService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/workers")
@CrossOrigin
public class WorkerController {
    private final WorkerRepository repository;
    private final OperationLogService logService;

    public WorkerController(WorkerRepository repository, OperationLogService logService) {
        this.repository = repository;
        this.logService = logService;
    }

    @GetMapping
    public List<Worker> all() {
        return repository.findAll();
    }

    @GetMapping("/search")
    public List<Worker> search(@RequestParam String term) {
        String q = term.trim();
        if (q.isEmpty()) {
            return repository.findAll();
        }
        return repository.findByCodeContainingIgnoreCaseOrNameContainingIgnoreCase(q, q);
    }

    @GetMapping("/code/{code}")
    public org.springframework.http.ResponseEntity<Worker> byCode(@PathVariable String code) {
        Worker w = repository.findByCode(code);
        // always return 200 so the frontend doesn't treat missing workers as errors
        return org.springframework.http.ResponseEntity.ok().body(w);
    }

    @PostMapping
    public Worker create(@RequestBody Worker worker, @RequestHeader("X-User") String user) {
        validate(worker);
        Worker saved = repository.save(worker);
        logService.log(user, "新增人员 " + worker.getName(), "id=" + saved.getId());
        return saved;
    }

    @PutMapping("/{id}")
    public Worker update(@PathVariable Long id, @RequestBody Worker worker, @RequestHeader("X-User") String user) {
        worker.setId(id);
        validate(worker);
        Worker saved = repository.save(worker);
        logService.log(user, "更新人员 " + id, null);
        return saved;
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id, @RequestHeader("X-User") String user) {
        repository.deleteById(id);
        logService.log(user, "删除人员 " + id, null);
    }

    private void validate(Worker worker) {
        if (worker.getCode() == null || worker.getCode().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "工号不能为空");
        }
        if (worker.getName() == null || worker.getName().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "姓名不能为空");
        }
    }
}
