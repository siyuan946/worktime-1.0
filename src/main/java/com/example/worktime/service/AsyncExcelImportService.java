package com.example.worktime.service;

import com.example.worktime.model.ImportJob;
import com.example.worktime.model.JobStatus;
import com.example.worktime.model.UploadedFile;
import com.example.worktime.model.WorkRecord;
import com.example.worktime.repository.ImportJobRepository;
import com.example.worktime.repository.UploadedFileRepository;
import com.example.worktime.repository.WorkRecordRepository;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.xssf.eventusermodel.ReadOnlySharedStringsTable;
import org.apache.poi.xssf.eventusermodel.XSSFReader;
import org.apache.poi.xssf.eventusermodel.XSSFSheetXMLHandler;
import org.apache.poi.xssf.model.StylesTable;
import org.apache.poi.xssf.usermodel.XSSFComment;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;


@Service
public class AsyncExcelImportService {

    private final UploadedFileRepository fileRepo;
    private final WorkRecordRepository recordRepo;
    private final ProcessCodeService processService;
    private final ImportJobRepository jobRepo;
    private final ThreadPoolTaskExecutor importExecutor;

    @PersistenceContext
    private EntityManager em;

    public AsyncExcelImportService(UploadedFileRepository fileRepo,
                                   WorkRecordRepository recordRepo,
                                   ProcessCodeService processService,
                                   ImportJobRepository jobRepo,
                                   ThreadPoolTaskExecutor importExecutor) {
        this.fileRepo = fileRepo;
        this.recordRepo = recordRepo;
        this.processService = processService;
        this.jobRepo = jobRepo;
        this.importExecutor = importExecutor;
    }

    /** 为了 Java 8 兼容，避免用 BiConsumer，定义一个简单的行处理接口 */
    private interface RowHandler {
        void handle(int rowNum, List<String> columns);
    }

    @Transactional
    public ImportJob startImport(String fileName, byte[] data) {
        UploadedFile uf = new UploadedFile();
        uf.setFileName(fileName);
        uf.setData(data);
        uf.setUploadTime(java.time.LocalDateTime.now());
        uf = fileRepo.saveAndFlush(uf);

        ImportJob job = ImportJob.create(uf.getId());
        jobRepo.saveAndFlush(job);

        final long fileId = uf.getId();
        final byte[] content = data;

        importExecutor.submit(new Runnable() {
            @Override public void run() {
                doImport(job.getId(), fileId, content);
            }
        });

        return job;
    }

    @Transactional(readOnly = true)
    public ImportJob getJob(String jobId) {
        return jobRepo.findById(jobId).orElse(null);
    }

    /** 真正导入逻辑：SAX 流式解析 + 批量入库（Java 8 友好） */
    private void doImport(String jobId, long fileId, byte[] content) {
        ImportJob job = jobRepo.findById(jobId).orElseThrow(IllegalStateException::new);
        job.setStatus(JobStatus.RUNNING);
        jobRepo.save(job);

        final AtomicLong processed = new AtomicLong(0L);
        final AtomicLong written   = new AtomicLong(0L);

        final int batchSize = 1000;
        final Map<String,String> codeCache = processService.loadCacheSnapshot();
        final List<WorkRecord> buffer = new ArrayList<WorkRecord>(batchSize * 2);
        final java.time.LocalDateTime uploadTime = fileRepo.findById(fileId).map(UploadedFile::getUploadTime).orElse(null);

        try (InputStream in = new ByteArrayInputStream(content)) {

            parseTargetSheet(in, "计划表", new RowHandler() {
                @Override
                public void handle(int rowNum, List<String> cols) {
                    // 跳过表头（第1行）
                    if (rowNum == 1) return;

                    String drawing  = cell(cols, 4);   // E
                    String prodName = cell(cols, 5);   // F
                    Integer qty     = toInt(cell(cols, 1)); // B
                    String notif    = cell(cols, 42);  // AQ

                    // J..AC（9..28）成对：工序名 / 工时
                    for (int c = 9; c <= 28; c += 2) {
                        String processName = cell(cols, c);
                        String hoursStr    = cell(cols, c + 1);
                        if ((processName == null || processName.length() == 0) &&
                                (hoursStr == null || hoursStr.length() == 0)) {
                            continue;
                        }

                        Double hours = toDouble(hoursStr);

                        WorkRecord wr = new WorkRecord();
                        // 使用 getReference 避免加载整个文件实体
                        UploadedFile fileRef = em.getReference(UploadedFile.class, fileId);
                        wr.setFile(fileRef);

                        wr.setNotificationNumber(notif);
                        wr.setProductName(prodName);
                        wr.setDrawingNumber(drawing);
                        wr.setPartName(prodName);
                        wr.setPlanQty(qty);
                        wr.setSourceRowNumber(rowNum);
                        wr.setProcessName(processName);

                        String normalized = (processName == null) ? null : processName.trim();
                        String code = null;
                        if (normalized != null && normalized.length() > 0) {
                            code = codeCache.get(normalized);
                            if (code == null) {
                                String found = processService.getCode(normalized);
                                if (found != null && found.trim().length() > 0) {
                                    code = found.trim();
                                    codeCache.put(normalized, code);
                                }
                            }
                        }
                        boolean codeMissing = (code == null || code.length() == 0);
                        if (codeMissing) {
                            code = (normalized != null && normalized.length() > 0) ? normalized : processName;
                        }
                        wr.setProcessCode(code);
                        wr.setCodeMissing(Boolean.valueOf(codeMissing));

                        if (drawing != null && notif != null && code != null) {
                            String bar = (drawing + "-" + notif + "-" + code)
                                    .replaceAll("[^\\x00-\\x7F]", "")
                                    .replaceAll("\\s+", "");
                            wr.setBarcode(bar);
                        }

                        wr.setHours(hours);
                        wr.setHoursMissing(Boolean.valueOf(hours == null));
                        wr.setFilled(Boolean.FALSE);

                        YearMonth ym = determineNaturalMonthLikeController(wr, uploadTime);
                        wr.setNaturalMonth(ym != null ? ym.toString() : null);

                        buffer.add(wr);
                        if (buffer.size() >= batchSize) {
                            recordRepo.saveAll(buffer);
                            recordRepo.flush();
                            buffer.clear();
                            if (em != null) em.clear();
                            // 注意：这里统计的是写入条数（不是原始行数）
                            written.addAndGet(batchSize);

                        }
                    }

                    // 原始行计数
                    incrementProcessed(jobId, processed.incrementAndGet(), written.get() + buffer.size());

                }
            });

            if (!buffer.isEmpty()) {
                recordRepo.saveAll(buffer);
                recordRepo.flush();
                if (em != null) em.clear();
                written.addAndGet(buffer.size());

                buffer.clear();
            }

            ImportJob done = jobRepo.findById(jobId).orElseThrow(IllegalStateException::new);
            done.setProcessedRows(processed.get());
            done.setOutputRows(written.get());

            done.setStatus(JobStatus.COMPLETED);
            jobRepo.save(done);

        } catch (Exception e) {
            ImportJob fail = jobRepo.findById(jobId).orElseThrow(IllegalStateException::new);
            fail.setStatus(JobStatus.FAILED);
            fail.setMessage(e.getClass().getSimpleName() + ": " + (e.getMessage() == null ? "" : e.getMessage()));
            jobRepo.save(fail);
        }
    }

    private void incrementProcessed(String jobId, long processed, long output) {
        if (processed % 1000 == 0) {
            ImportJob j = jobRepo.findById(jobId).orElse(null);
            if (j != null) {
                j.setProcessedRows(processed);
                j.setOutputRows(output);
                jobRepo.save(j);
            }
        }
    }

    /** 解析“计划表”优先；找不到则解析第一张 */
    private void parseTargetSheet(InputStream in, String targetSheetName, final RowHandler rowHandler) throws Exception {
        final String NS_MAIN = "http://schemas.openxmlformats.org/spreadsheetml/2006/main";
        final String NS_REL  = "http://schemas.openxmlformats.org/officeDocument/2006/relationships";

        OPCPackage pkg = OPCPackage.open(in);
        try {
            XSSFReader reader = new XSSFReader(pkg);
            StylesTable styles = reader.getStylesTable();
            ReadOnlySharedStringsTable sst = new ReadOnlySharedStringsTable(pkg);

            String rId = findSheetRIdByName(reader, targetSheetName, NS_MAIN, NS_REL);

            final DataFormatter fmt = new DataFormatter(); // Java 8 OK
            XMLReader parser = XMLReaderFactory.createXMLReader();

            XSSFSheetXMLHandler.SheetContentsHandler handler = new XSSFSheetXMLHandler.SheetContentsHandler() {
                List<String> cells = new ArrayList<String>(64);
                int curRow = -1;
                int lastCol = -1;

                @Override public void startRow(int rowNum) {
                    curRow = rowNum + 1; // 行号从1开始
                    cells.clear();
                    lastCol = -1;
                }

                @Override public void endRow(int rowNum) {
                    rowHandler.handle(curRow, new ArrayList<String>(cells));
                }

                @Override public void cell(String cellRef, String formattedValue, XSSFComment comment) {
                    int colIdx = colIndex(cellRef);
                    for (int i = lastCol + 1; i < colIdx; i++) {
                        cells.add("");
                    }
                    cells.add(formattedValue == null ? "" : formattedValue);
                    lastCol = colIdx;
                }

                @Override public void headerFooter(String text, boolean isHeader, String tagName) {}
            };

            // 注意：POI 不同版本构造器略有差异；这版在 3.17 / 4.x / 5.x 都可用
            XSSFSheetXMLHandler xssh = new XSSFSheetXMLHandler(styles, null, sst, handler, fmt, false);
            parser.setContentHandler(xssh);

            if (rId != null) {
                InputStream sheet = reader.getSheet(rId);
                try {
                    parser.parse(new InputSource(sheet));
                } finally {
                    if (sheet != null) sheet.close();
                }
                return;
            }

            // fallback：第一张
            Iterator<InputStream> it = reader.getSheetsData();
            if (it != null && it.hasNext()) {
                InputStream first = it.next();
                try {
                    parser.parse(new InputSource(first));
                } finally {
                    if (first != null) first.close();
                }
            }
        } finally {
            pkg.close();
        }
    }

    /** 从 workbook.xml 解析 name -> r:id，Java 8 版 */
    private String findSheetRIdByName(XSSFReader reader, String target,
                                      String nsMain, String nsRel) {
        InputStream wb = null;
        try {
            wb = reader.getWorkbookData();
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true); // 关键：要命名空间感知
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(wb);
            NodeList sheets = doc.getElementsByTagNameNS(nsMain, "sheet");
            for (int i = 0; i < sheets.getLength(); i++) {
                Element e = (Element) sheets.item(i);
                String name = e.getAttribute("name");
                String rid  = e.getAttributeNS(nsRel, "id");
                if (name != null && name.trim().equals(target) && rid != null && rid.length() > 0) {
                    return rid;
                }
            }
        } catch (Exception ignore) {
            // 忽略，回退用第一张
        } finally {
            try { if (wb != null) wb.close(); } catch (Exception ignored) {}
        }
        return null;
    }

    private static String cell(List<String> cols, int idx) {
        if (idx < 0 || idx >= cols.size()) return null;
        String v = cols.get(idx);
        if (v == null) return null;
        String t = v.trim();
        return t.length() == 0 ? null : t;
    }

    private static Integer toInt(String s) {
        if (s == null) return null;
        try { return new BigDecimal(s.trim()).intValue(); } catch (Exception e) { return null; }
    }

    private static Double toDouble(String s) {
        if (s == null) return null;
        try { return new BigDecimal(s.trim()).doubleValue(); } catch (Exception e) { return null; }
    }

    /** 列引用转索引：AC27 -> 0-based 列号 */
    private static int colIndex(String cellRef) {
        int i = 0, col = 0;
        while (i < cellRef.length()) {
            char ch = cellRef.charAt(i);
            if (ch >= 'A' && ch <= 'Z') { col = col * 26 + (ch - 'A' + 1); i++; }
            else if (ch >= 'a' && ch <= 'z') { col = col * 26 + (ch - 'a' + 1); i++; }
            else break;
        }
        return col - 1;
    }

    /** 复刻 Controller 的自然月逻辑（缺开始/结束时回退到上传时间） */
    private YearMonth determineNaturalMonthLikeController(WorkRecord record, java.time.LocalDateTime uploadTime) {
        LocalDate date = null;
        if (record.getEndTime() != null)       date = record.getEndTime().toLocalDate();
        else if (record.getStartTime() != null)date = record.getStartTime().toLocalDate();
        else if (uploadTime != null)           date = uploadTime.toLocalDate();
        if (date == null) return null;
        if (date.getDayOfMonth() >= 26) date = date.plusMonths(1);
        return YearMonth.from(date);
    }
}
