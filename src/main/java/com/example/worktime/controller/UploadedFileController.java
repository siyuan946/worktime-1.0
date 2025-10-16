package com.example.worktime.controller;

import com.example.worktime.model.UploadedFile;
import com.example.worktime.repository.UploadedFileRepository;
import com.example.worktime.repository.WorkRecordRepository;
import com.example.worktime.service.OperationLogService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/files")
@CrossOrigin
public class UploadedFileController {
    private final UploadedFileRepository repository;
    private final WorkRecordRepository recordRepository;
    private final OperationLogService logService;

    public UploadedFileController(UploadedFileRepository repository, WorkRecordRepository recordRepository,
                                  OperationLogService logService) {
        this.repository = repository;
        this.recordRepository = recordRepository;
        this.logService = logService;
    }

    @GetMapping
    public List<UploadedFile> all() {
        return repository.findAllWithRecords();
    }

    @DeleteMapping("/{id}")
    @Transactional
    public void delete(@PathVariable Long id, @RequestHeader("X-User") String user) {
        UploadedFile file = repository.findById(id).orElse(null);
        long cnt = recordRepository.countByFileId(id);
        recordRepository.deleteByFileId(id);
        repository.deleteById(id);
        String name = file != null ? file.getFileName() : String.valueOf(id);
        logService.log(user, "删除文件 " + name, "records=" + cnt);
    }
}