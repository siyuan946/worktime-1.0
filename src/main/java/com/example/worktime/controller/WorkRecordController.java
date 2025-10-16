package com.example.worktime.controller;

import com.example.worktime.model.WorkRecord;
import com.example.worktime.model.UploadedFile;
import com.example.worktime.repository.WorkRecordRepository;
import com.example.worktime.repository.UploadedFileRepository;
import com.example.worktime.service.OperationLogService;
import com.example.worktime.service.ProcessCodeService;
import com.example.worktime.service.WorkerService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;
import com.example.worktime.model.Worker;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.oned.Code128Writer;
import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;

import com.example.worktime.service.AsyncExcelImportService;
import com.example.worktime.model.ImportJob;

@RestController
@RequestMapping("/api/workrecords")
@CrossOrigin
public class WorkRecordController {
    private final WorkRecordRepository repository;
    private final ProcessCodeService processService;
    private final WorkerService workerService;
    private final UploadedFileRepository fileRepository;
    private final OperationLogService logService;
    private final AsyncExcelImportService asyncExcelImportService; // 新增


    @PersistenceContext
    private EntityManager entityManager;

    public WorkRecordController(WorkRecordRepository repository,
                                ProcessCodeService processService,
                                WorkerService workerService,
                                UploadedFileRepository fileRepository,
                                OperationLogService logService,
                                AsyncExcelImportService asyncExcelImportService) { // 新增
        this.repository = repository;
        this.processService = processService;
        this.workerService = workerService;
        this.fileRepository = fileRepository;
        this.logService = logService;
        this.asyncExcelImportService = asyncExcelImportService; // 新增
    }

    @GetMapping
    public List<WorkRecord> all() {
        return repository.findAll();
    }

    @GetMapping("/barcode/{barcode}")
    public List<WorkRecord> byBarcode(@PathVariable String barcode) {
        String clean = sanitizeBarcode(barcode);
        return repository.findByBarcode(clean);
    }

    @GetMapping("/file/{fileId}")
    public List<WorkRecord> byFile(@PathVariable Long fileId) {
        return repository.findByFileId(fileId);
    }

    @GetMapping("/file/{fileId}/drawings")
    public List<Map<String, Object>> drawingBuckets(@PathVariable Long fileId) {
        List<Object[]> raw = repository.findDrawingBuckets(fileId);
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object[] row : raw) {
            Map<String, Object> map = new HashMap<>();
            map.put("drawing", row[0]);
            map.put("count", row[1] instanceof Number ? ((Number) row[1]).longValue() : 0L);
            if (row.length > 2 && row[2] instanceof Number) {
                map.put("startRow", ((Number) row[2]).longValue());
            }
            result.add(map);
        }
        return result;
    }

    @GetMapping("/file/{fileId}/page")
    public Page<WorkRecord> pageByDrawing(@PathVariable Long fileId,
                                          @RequestParam String drawing,
                                          @RequestParam(defaultValue = "0") int page,
                                          @RequestParam(defaultValue = "200") int size) {
        int pageSize = Math.max(50, Math.min(size, 500));
        Pageable pageable = PageRequest.of(Math.max(page, 0), pageSize);
        Page<WorkRecord> result = repository.findByFileIdAndDrawingNumber(fileId, drawing, pageable);
        result.forEach(r -> r.setBarcodeImage(null));
        return result;
    }

    @GetMapping("/file/{fileId}/filled")
    public List<WorkRecord> byFileFilled(@PathVariable Long fileId) {
        return repository.findByFileIdAndFilledTrue(fileId);
    }

    @GetMapping("/file/{fileId}/export")
    public void exportFilled(@PathVariable Long fileId, HttpServletResponse response) throws IOException {
        List<WorkRecord> list = repository.findByFileIdAndFilledTrue(fileId);
        String name = fileRepository.findById(fileId).map(UploadedFile::getFileName).orElse("records.xlsx");
        exportList(list, name, response);
    }

    @GetMapping("/date/{date}/export")
    public void exportByDate(@PathVariable @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate date,
                             HttpServletResponse response) throws IOException {
        java.time.LocalDateTime start = date.atStartOfDay();
        java.time.LocalDateTime end = start.plusDays(1);
        List<WorkRecord> list = repository.findByUploadDate(start, end);
        exportList(list, "records_" + date.toString() + ".xlsx", response);
    }

    @GetMapping("/natural-month/{year}/{month}/export")
    @Transactional(readOnly = true)
    public void exportByNaturalMonth(@PathVariable int year,
                                     @PathVariable int month,
                                     HttpServletResponse response) throws IOException {
        YearMonth ym;
        try {
            ym = YearMonth.of(year, month);
        } catch (DateTimeException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "无效的月份");
        }
        List<WorkRecord> filled = repository.findByFilledTrue();
        List<WorkRecord> filtered = new ArrayList<>();
        for (WorkRecord record : filled) {
            YearMonth recordMonth = determineNaturalMonth(record);
            if (ym.equals(recordMonth)) {
                filtered.add(record);
            }
        }
        String fileName = String.format("records_%s.xlsx", ym);
        exportList(filtered, fileName, response);
    }

    @GetMapping("/drawing/{drawing}/export")
    @Transactional(readOnly = true)
    public void exportByDrawing(@PathVariable("drawing") String drawing,
                                HttpServletResponse response) throws IOException {
        if (drawing == null || drawing.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "图号不能为空");
        }
        String normalized = drawing.trim();
        List<WorkRecord> list = repository.findByDrawingNumber(normalized);
        String sanitized = normalized.replaceAll("[\\\\/:*?\"<>|]", "_");
        if (sanitized.isEmpty()) {
            sanitized = "records";
        }
        String fileName = sanitized.endsWith(".xlsx") ? sanitized : sanitized + ".xlsx";
        exportList(list, fileName, response);
    }

    private YearMonth determineNaturalMonth(WorkRecord record) {
        LocalDate date = null;
        if (record.getEndTime() != null) {
            date = record.getEndTime().toLocalDate();
        } else if (record.getStartTime() != null) {
            date = record.getStartTime().toLocalDate();
        } else if (record.getFile() != null && record.getFile().getUploadTime() != null) {
            date = record.getFile().getUploadTime().toLocalDate();
        }
        if (date == null) return null;
        if (date.getDayOfMonth() >= 26) {
            date = date.plusMonths(1);
        }
        return YearMonth.from(date);
    }

    private java.util.List<String> splitWorkers(String codes) {
        if (codes == null || codes.trim().isEmpty()) return java.util.Collections.emptyList();
        String[] arr = codes.split("[,\u3001\\s]+");
        java.util.List<String> list = new java.util.ArrayList<>();
        for (String s : arr) if (!s.trim().isEmpty()) list.add(s.trim());
        return list;
    }

    private java.util.List<String> splitNames(String names) {
        if (names == null || names.trim().isEmpty()) return java.util.Collections.emptyList();
        String[] arr = names.split("[,\u3001\\s]+");
        java.util.List<String> list = new java.util.ArrayList<>();
        for (String s : arr) if (!s.trim().isEmpty()) list.add(s.trim());
        return list;
    }

    private java.util.List<Double> parseQtys(String str) {
        java.util.List<Double> vals = new java.util.ArrayList<>();
        if (str == null) return vals;
        for (String seg : str.trim().split("[\\s,]+")) {
            if (seg.isEmpty()) continue;
            int idx = seg.indexOf(":");
            if (idx < 0) idx = seg.indexOf('：');
            String num = idx >= 0 ? seg.substring(idx + 1) : seg;
            if (num.isEmpty()) { vals.add(null); continue; }
            try { vals.add(Double.parseDouble(num)); } catch (NumberFormatException e) { vals.add(null); }
        }
        return vals;
    }

    private void exportList(java.util.List<WorkRecord> list, String fileName, HttpServletResponse response) throws IOException {
        Workbook wb = new org.apache.poi.xssf.usermodel.XSSFWorkbook();
        Sheet sheet = wb.createSheet("records");
        CellStyle twoDec = wb.createCellStyle();
        twoDec.setDataFormat(wb.createDataFormat().getFormat("0.00"));
        Row head = sheet.createRow(0);
        String[] titles = {"日期","通知单号","人员代码","人数","图号","工序代码","数量分配","单件工时","劳资系数","分配调整","单件工时分配","姓名","计划数","产品名称"};
        for (int i = 0; i < titles.length; i++) {
            head.createCell(i).setCellValue(titles[i]);
        }

        int rowIdx = 1;
        for (WorkRecord r : list) {
            java.util.List<String> codes = splitWorkers(r.getWorkerCodes());
            java.util.List<String> names = splitNames(r.getWorkerNames());
            java.util.List<Double> qtys = parseQtys(r.getWorkerQtys());
            int max = Math.max(1, Math.max(Math.max(codes.size(), names.size()), qtys.size()));
            int numWorkers = max;

            String timeCell = "";
            if (r.getStartTime() != null) {
                java.time.format.DateTimeFormatter fmt = java.time.format.DateTimeFormatter.ofPattern("M.d");
                String s = r.getStartTime().toLocalDate().format(fmt);
                if (r.getEndTime() != null && !r.getEndTime().toLocalDate().isEqual(r.getStartTime().toLocalDate())) {
                    String e = r.getEndTime().toLocalDate().format(fmt);
                    timeCell = s + "-" + e;
                } else {
                    timeCell = s;
                }
            } else if (r.getEndTime() != null) {
                java.time.format.DateTimeFormatter fmt = java.time.format.DateTimeFormatter.ofPattern("M.d");
                timeCell = r.getEndTime().toLocalDate().format(fmt);
            }

            for (int i = 0; i < max; i++) {
                Row row = sheet.createRow(rowIdx++);
                int c = 0;
                row.createCell(c++).setCellValue(timeCell);
                row.createCell(c++).setCellValue(n(r.getNotificationNumber()));
                row.createCell(c++).setCellValue(i < codes.size() ? n(codes.get(i)) : "");
                row.createCell(c++).setCellValue(numWorkers);
                row.createCell(c++).setCellValue(n(r.getDrawingNumber()));
                row.createCell(c++).setCellValue(n(r.getProcessCode()));
                Double q = i < qtys.size() ? qtys.get(i) : null;
                if (q != null) {
                    Cell cell = row.createCell(c++);
                    cell.setCellValue(q);
                    cell.setCellStyle(twoDec);
                }
                else row.createCell(c++).setCellValue("");
                if (r.getHours() != null) {
                    Cell cell = row.createCell(c++);
                    cell.setCellValue(r.getHours());
                    cell.setCellStyle(twoDec);
                } else row.createCell(c++).setCellValue("");
                row.createCell(c++).setCellValue("");
                row.createCell(c++).setCellValue("");
                Double workerHours = null;
                if (q != null && r.getHours() != null) workerHours = q * r.getHours();
                if (workerHours != null) {
                    Cell cell = row.createCell(c++);
                    cell.setCellValue(workerHours);
                    cell.setCellStyle(twoDec);
                }
                else row.createCell(c++).setCellValue("");
                row.createCell(c++).setCellValue(i < names.size() ? n(names.get(i)) : "");
                if (r.getPlanQty() != null) row.createCell(c++).setCellValue(r.getPlanQty()); else row.createCell(c++).setCellValue("");
                row.createCell(c++).setCellValue(n(r.getProductName()));
            }
        }
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        String safeName = (fileName == null || fileName.isEmpty()) ? "records.xlsx" : fileName;
        String encoded = java.net.URLEncoder.encode(safeName, String.valueOf(StandardCharsets.UTF_8)).replace("+", "%20");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + encoded + "\"; filename*=UTF-8''" + encoded);
        wb.write(response.getOutputStream());
        wb.close();
    }

    @GetMapping("/file/{fileId}/print")
    public void printFile(@PathVariable Long fileId, HttpServletResponse response) throws IOException {
        List<WorkRecord> list = repository.findByFileId(fileId);
        ClassPathResource res = new ClassPathResource("template.xlsx");
        try (InputStream in = res.getInputStream(); Workbook wb = WorkbookFactory.create(in)) {
            Sheet sheet = wb.getSheetAt(0);
            int rowIdx = 1;
            Drawing<?> drawing = sheet.createDrawingPatriarch();
            CreationHelper helper = wb.getCreationHelper();
            for (WorkRecord r : list) {
                Row row = sheet.getRow(rowIdx);
                if (row == null) row = sheet.createRow(rowIdx);
                int c = 0;
                row.createCell(c++).setCellValue(rowIdx);
                row.createCell(c++).setCellValue(n(r.getNotificationNumber()));
                row.createCell(c++).setCellValue(n(r.getProductName()));
                row.createCell(c++).setCellValue(n(r.getDrawingNumber()));
                row.createCell(c++).setCellValue(n(r.getProcessCode()));
                if (r.getBarcodeImage() != null) {
                    int picId = wb.addPicture(r.getBarcodeImage(), Workbook.PICTURE_TYPE_PNG);
                    ClientAnchor anchor = helper.createClientAnchor();
                    anchor.setRow1(rowIdx);
                    anchor.setCol1(c);
                    Picture pic = drawing.createPicture(anchor, picId);
                    pic.resize();
                }
                rowIdx++;
            }
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition","attachment; filename=print.xlsx");
            wb.write(response.getOutputStream());
        }
    }

    @GetMapping("/generateBarcode")
    public String generateBarcodeEndpoint(@RequestParam("text") String text) {
        byte[] img = generateBarcode(sanitizeBarcode(text));
        return img == null ? null : java.util.Base64.getEncoder().encodeToString(img);
    }

    @PostMapping("/generateBarcodes")
    public Map<String, String> generateBarcodes(@RequestBody List<String> barcodes) {
        Map<String, String> result = new LinkedHashMap<>();
        if (barcodes == null) {
            return result;
        }
        for (String raw : barcodes) {
            if (raw == null) continue;
            String clean = sanitizeBarcode(raw);
            if (clean == null || clean.isEmpty() || result.containsKey(clean)) {
                continue;
            }
            byte[] img = generateBarcode(clean);
            if (img != null) {
                result.put(clean, java.util.Base64.getEncoder().encodeToString(img));
            }
        }
        return result;
    }

    @PutMapping("/bulk")
    @Transactional
    public List<WorkRecord> bulkUpdate(@RequestBody List<WorkRecord> records,
                                       @RequestHeader("X-User") String user) {
        if (records == null || records.isEmpty()) {
            return Collections.emptyList();
        }
        List<WorkRecord> result = new ArrayList<>();
        for (WorkRecord record : records) {
            if (record == null || record.getId() == null) {
                continue;
            }
            WorkRecord existing = repository.findById(record.getId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "record not found: " + record.getId()));
            if (record.getFile() == null) {
                record.setFile(existing.getFile());
            }
            if (record.getBarcode() == null) record.setBarcode(existing.getBarcode());
            if (record.getBarcodeImage() == null) record.setBarcodeImage(existing.getBarcodeImage());
            prepare(record);
            YearMonth ym = determineNaturalMonth(record);
            record.setNaturalMonth(ym != null ? ym.toString() : null);
            if (record.getQualifiedQty() != null) record.setFilled(true);
            result.add(repository.save(record));
        }
        repository.flush();
        logService.log(user, "批量更新记录", "count=" + result.size());
        return result;
    }

    @PutMapping("/{id}")
    @Transactional
    public WorkRecord update(@PathVariable Long id, @RequestBody WorkRecord record,
                             @RequestHeader("X-User") String user) {
        WorkRecord existing = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "record not found"));
        record.setId(id);
        if (record.getFile() == null) {
            record.setFile(existing.getFile());
        }
        if (record.getBarcode() == null) record.setBarcode(existing.getBarcode());
        if (record.getBarcodeImage() == null) record.setBarcodeImage(existing.getBarcodeImage());
        prepare(record);
        YearMonth ym = determineNaturalMonth(record);
        record.setNaturalMonth(ym != null ? ym.toString() : null);
        if (record.getQualifiedQty() != null) record.setFilled(true);
        WorkRecord updated = repository.save(record);
        logService.log(user, "更新记录 " + id, null);
        return updated;
    }

    @PostMapping("/duplicate/{id}")
    public WorkRecord duplicate(@PathVariable Long id, @RequestHeader("X-User") String user) {
        WorkRecord src = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "record not found"));
        WorkRecord copy = new WorkRecord();
        copy.setNotificationNumber(src.getNotificationNumber());
        copy.setProductName(src.getProductName());
        copy.setDrawingNumber(src.getDrawingNumber());
        copy.setProductCode(src.getProductCode());
        copy.setPartName(src.getPartName());
        copy.setPlanQty(src.getPlanQty());
        copy.setProcessName(src.getProcessName());
        copy.setProcessCode(src.getProcessCode());
        copy.setBarcode(src.getBarcode());
        copy.setBarcodeImage(src.getBarcodeImage());
        copy.setBatchNumber(src.getBatchNumber());
        copy.setHours(src.getHours());
        copy.setFile(src.getFile());
        copy.setSupplemental(true);
        copy.setFilled(false);
        prepare(copy);
        YearMonth ym = determineNaturalMonth(copy);
        copy.setNaturalMonth(ym != null ? ym.toString() : null);
        WorkRecord saved = repository.save(copy);
        logService.log(user, "复制记录 " + id, "newId=" + saved.getId());
        return saved;
    }

    @DeleteMapping("/{id}")
    @Transactional
    public void delete(@PathVariable Long id, @RequestHeader("X-User") String user) {
        repository.deleteById(id);
        repository.flush();
        logService.log(user, "删除记录 " + id, null);
    }

    @PostMapping
    @org.springframework.transaction.annotation.Transactional
    public List<WorkRecord> save(@RequestParam("fileId") Long fileId, @RequestBody List<WorkRecord> records,
                                 @RequestHeader("X-User") String user) {
        UploadedFile file = fileRepository.findById(fileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "无效文件"));
        for (WorkRecord r : records) {
            r.setFile(file);
            r.setFilled(false);
            prepare(r);
            YearMonth ym = determineNaturalMonth(r);
            r.setNaturalMonth(ym != null ? ym.toString() : null);
            r.setSupplemental(!repository.findByBarcode(r.getBarcode()).isEmpty());
        }
        java.util.List<WorkRecord> saved = repository.saveAll(records);
        repository.flush();
        System.out.println("Saved records: " + saved.size());
        logService.log(user, "新增记录" , "fileId=" + fileId + " count=" + saved.size());
        return saved;
    }

    @PostMapping("/import")
    public Map<String, Object> startImport(@RequestParam("file") MultipartFile file,
                                           @RequestHeader("X-User") String user) throws Exception {
        byte[] data = file.getBytes();
        com.example.worktime.model.ImportJob job = asyncExcelImportService.startImport(file.getOriginalFilename(), data);
        logService.log(user, "上传文件(异步导入) " + file.getOriginalFilename(),
                "jobId=" + job.getId() + ", fileId=" + job.getFileId());
        Map<String,Object> resp = new HashMap<>();
        resp.put("jobId", job.getId());
        resp.put("fileId", job.getFileId());
        return resp;
    }

    @GetMapping("/import/{jobId}/status")
    public Map<String, Object> importStatus(@PathVariable String jobId) {
        com.example.worktime.model.ImportJob job = asyncExcelImportService.getJob(jobId);
        if (job == null) throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.NOT_FOUND, "job not found");
        Map<String,Object> resp = new HashMap<>();
        resp.put("jobId", job.getId());
        resp.put("fileId", job.getFileId());
        resp.put("status", job.getStatus().name());
        resp.put("processedRows", job.getProcessedRows());
        resp.put("outputRows", job.getOutputRows());
        resp.put("message", job.getMessage());
        return resp;
    }




    private List<WorkRecord> parseExcel(MultipartFile file, String password) throws IOException {
        List<WorkRecord> result = new ArrayList<>();
        try (Workbook wb = openWorkbook(file, password)) {
            Sheet sheet = resolvePlanSheet(wb);
            if (sheet.getPhysicalNumberOfRows() < 1) return result;

            // Fixed column indexes: F=5, E=4, J=9 ... AC=28, AQ=42
            Map<String, String> codeCache = processService.loadCacheSnapshot();
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                String notification = getString(row, 42);   // AQ
                String prodName = getString(row, 5);        // F
                String drawing = getString(row, 4);         // E
                Integer qty = getInt(row, 1);               // B -> 计划数

                for (int c = 9; c <= 28; c += 2) {          // J..AC pairs
                    String process = getString(row, c);
                    String normalizedProcess = process != null ? process.trim() : null;
                    Double hours = getDouble(row, c + 1);
                    if ((normalizedProcess == null || normalizedProcess.isEmpty()) && hours == null) continue;

                    WorkRecord wr = new WorkRecord();
                    wr.setNotificationNumber(notification);
                    wr.setProductName(prodName);
                    wr.setDrawingNumber(drawing);
                    wr.setPartName(prodName);
                    wr.setPlanQty(qty);
                    wr.setSourceRowNumber(i + 1);
                    wr.setProcessName(process);

                    String code = null;
                    if (normalizedProcess != null && !normalizedProcess.isEmpty()) {
                        code = codeCache.get(normalizedProcess);
                        if (code == null) {
                            code = processService.getCode(normalizedProcess);
                            if (code != null && !code.trim().isEmpty()) {
                                code = code.trim();
                                codeCache.put(normalizedProcess, code);
                            }
                        }
                    }
                    boolean codeMissing = false;
                    if (code == null || code.trim().isEmpty()) {
                        code = normalizedProcess != null && !normalizedProcess.isEmpty() ? normalizedProcess : process;
                        codeMissing = true;
                    }
                    wr.setProcessCode(code);
                    wr.setCodeMissing(codeMissing);

                    if (drawing != null && notification != null && code != null) {
                        String bar = drawing + "-" + notification + "-" + code;
                        String clean = sanitizeBarcode(bar);
                        wr.setBarcode(clean);
                    }

                    wr.setHours(hours);
                    wr.setHoursMissing(hours == null);
                    wr.setFilled(false);
                    result.add(wr);
                }
            }
        }
        return result;
    }

    private Workbook openWorkbook(MultipartFile file, String password) throws IOException {
        try (InputStream is = file.getInputStream()) {
            Workbook wb;
            if (password != null && !password.trim().isEmpty()) {
                wb = WorkbookFactory.create(is, password);
            } else {
                wb = WorkbookFactory.create(is);
            }
            return wb;
        } catch (EncryptedDocumentException ex) {
            if (password == null || password.trim().isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "该文件已加密，请提供密码");
            }
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "文件密码错误或无法解密");
        }
    }

    private Sheet resolvePlanSheet(Workbook wb) {
        Sheet sheet = wb.getSheet("计划表");
        if (sheet != null) {
            return sheet;
        }
        for (int i = 0; i < wb.getNumberOfSheets(); i++) {
            Sheet candidate = wb.getSheetAt(i);
            if (candidate != null) {
                String name = candidate.getSheetName();
                if (name != null && "计划表".equals(name.trim())) {
                    return candidate;
                }
            }
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "未找到名称为“计划表”的工作表");
    }

    private final DataFormatter formatter = new DataFormatter();

    private String getString(Row row, Integer idx) {
        if (idx == null) return null;
        Cell c = row.getCell(idx);
        if (c == null) return null;
        String value = formatter.formatCellValue(c);
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private Double getDouble(Row row, Integer idx) {
        if (idx == null) return null;
        Cell c = row.getCell(idx);
        if (c == null) return null;
        String value = formatter.formatCellValue(c);
        if (value == null || value.trim().isEmpty()) return null;
        try {
            return new BigDecimal(value.trim()).doubleValue();
        } catch (NumberFormatException e) {
            return null;
        }
    }



    private Integer getInt(Row row, Integer idx) {
        Double d = getDouble(row, idx);
        return d == null ? null : d.intValue();
    }

    private void prepare(WorkRecord record) {
        prepare(record, true);
    }

    private void prepare(WorkRecord record, boolean strict) {
        if (record.getWorkerCodes() != null) {
            String[] codes = record.getWorkerCodes().split("[,\u3001\\p{Space}]+");
            List<String> names = new ArrayList<>();
            for (String c : codes) {
                if (c.trim().isEmpty()) continue;
                Worker w = workerService.getByCode(c.trim());
                if (w != null) names.add(w.getName());
            }
            record.setWorkerNames(String.join(",", names));
        }
        if (record.getQualifiedQty() != null && record.getHours() != null) {
            record.setHourSubtotal(record.getQualifiedQty() * record.getHours());
        }
        if (strict) {
            validate(record);
        }
    }

    private void validate(WorkRecord record) {
        if (record.getProcessCode() == null || record.getProcessCode().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "工序代码不能为空");
        }
        if (record.getBarcode() == null || record.getBarcode().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "条形码不能为空");
        }
    }

    private byte[] generateBarcode(String text) {
        try {
            Code128Writer writer = new Code128Writer();
            BitMatrix matrix = writer.encode(text, BarcodeFormat.CODE_128, 300, 80);
            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "png", out);
            return out.toByteArray();
        } catch (Exception e) {
            return null;
        }
    }

    private String sanitizeBarcode(String text) {
        if (text == null) return null;
        return text.replaceAll("[^\\x00-\\x7F]", "").replaceAll("\\s+", "");
    }

    private String n(String v) { return v == null ? "" : v; }
}