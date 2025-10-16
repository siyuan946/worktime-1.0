package com.example.worktime.controller;

import com.example.worktime.model.ProcessCode;
import com.example.worktime.repository.ProcessCodeRepository;
import org.springframework.http.HttpStatus;
import com.example.worktime.service.OperationLogService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/processcodes")
@CrossOrigin
public class ProcessCodeController {
    private final ProcessCodeRepository repository;
    private final OperationLogService logService;

    public ProcessCodeController(ProcessCodeRepository repository, OperationLogService logService) {
        this.repository = repository;
        this.logService = logService;
    }

    @GetMapping
    public List<ProcessCode> all() {
        return repository.findAll();
    }

    @GetMapping("/search")
    public List<ProcessCode> search(@RequestParam String term) {
        String q = term.trim();
        if (q.isEmpty()) {
            return repository.findAll();
        }
        return repository.findByCodeContainingIgnoreCaseOrNameContainingIgnoreCase(q, q);
    }

    @GetMapping("/name/{name}")
    public ProcessCode byName(@PathVariable String name) {
        return repository.findByName(name);
    }

    @PostMapping
    public ProcessCode create(@RequestBody ProcessCode pc, @RequestHeader("X-User") String user) {
        validate(pc);
        ProcessCode saved = repository.save(pc);
        logService.log(user, "新增工序 " + pc.getName(), "id=" + saved.getId());
        return saved;
    }

    @PutMapping("/{id}")
    public ProcessCode update(@PathVariable Long id, @RequestBody ProcessCode pc, @RequestHeader("X-User") String user) {
        pc.setId(id);
        validate(pc);
        ProcessCode saved = repository.save(pc);
        logService.log(user, "更新工序 " + id, null);
        return saved;
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id, @RequestHeader("X-User") String user) {
        repository.deleteById(id);
        logService.log(user, "删除工序 " + id, null);
    }

    private void validate(ProcessCode pc) {
        if (pc.getCode() == null || pc.getCode().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "工序代号不能为空");
        }

        if (pc.getName() == null || pc.getName().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "工序名称不能为空");
        }
    }
}
