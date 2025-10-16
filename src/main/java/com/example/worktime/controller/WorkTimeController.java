package com.example.worktime.controller;

import com.example.worktime.model.WorkStep;
import com.example.worktime.model.WorkTime;
import com.example.worktime.repository.WorkTimeRepository;
import org.apache.poi.ss.usermodel.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/api/worktimes")
@CrossOrigin
public class WorkTimeController {

    private final WorkTimeRepository repository;

    public WorkTimeController(WorkTimeRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<WorkTime> findAll() {
        return repository.findAll();
    }

    @PostMapping
    public WorkTime create(@RequestBody WorkTime workTime) {
        validate(workTime);
        return repository.save(workTime);
    }

    @PostMapping("/upload")
    public List<WorkTime> upload(@RequestParam("file") MultipartFile file) throws IOException {
        List<WorkTime> saved = new ArrayList<>();
        try (Workbook wb = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = wb.getSheetAt(0);
            if (sheet.getPhysicalNumberOfRows() < 1) {
                return saved;
            }
            Row header = sheet.getRow(0);
            Map<String, Integer> col = new HashMap<>();
            for (Cell cell : header) {
                col.put(cell.getStringCellValue().trim(), cell.getColumnIndex());
            }
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                WorkTime wt = new WorkTime();
                wt.setCode(getString(row, col.get("代号")));
                wt.setName(getString(row, col.get("名称")));
                wt.setStartDate(getDate(row, col.get("投产日期")));
                wt.setEndDate(getDate(row, col.get("完成日期")));
                List<WorkStep> steps = new ArrayList<>();
                int idx = 0;
                while (true) {
                    String pKey = idx == 0 ? "工序" : "工序." + idx;
                    String hKey = idx == 0 ? "单件工时" : "单件工时." + idx;
                    if (!col.containsKey(pKey) || !col.containsKey(hKey)) break;
                    String process = getString(row, col.get(pKey));
                    Double hours = getDouble(row, col.get(hKey));
                    if (process == null && hours == null) {
                        idx++; continue;
                    }
                    WorkStep step = new WorkStep();
                    step.setProcess(process);
                    step.setHours(hours);
                    step.setWorkTime(wt);
                    steps.add(step);
                    idx++;
                }
                wt.setSteps(steps);
                saved.add(repository.save(wt));
            }
        }
        return saved;
    }

    private String getString(Row row, Integer index) {
        if (index == null) return null;
        Cell cell = row.getCell(index);
        if (cell == null) return null;
        return cell.toString();
    }

    private Double getDouble(Row row, Integer index) {
        if (index == null) return null;
        Cell cell = row.getCell(index);
        if (cell == null) return null;
        return cell.getNumericCellValue();
    }

    private LocalDate getDate(Row row, Integer index) {
        if (index == null) return null;
        Cell cell = row.getCell(index);
        if (cell == null) return null;
        if (DateUtil.isCellDateFormatted(cell)) {
            return cell.getLocalDateTimeCellValue().toLocalDate();
        }
        return LocalDate.parse(cell.toString());
    }

    private void validate(WorkTime wt) {
        if (wt.getCode() == null || wt.getCode().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "代号不能为空");
        }
        if (wt.getName() == null || wt.getName().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "名称不能为空");
        }
    }
}