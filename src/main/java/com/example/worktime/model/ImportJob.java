package com.example.worktime.model;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "import_job")
public class ImportJob {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "file_id", nullable = false)
    private Long fileId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private JobStatus status;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private Long processedRows;
    private Long outputRows;

    @Column(length = 500)
    private String message;

    public static ImportJob create(Long fileId) {
        ImportJob j = new ImportJob();
        j.id = UUID.randomUUID().toString();
        j.fileId = fileId;
        j.status = JobStatus.PENDING;
        j.createdAt = LocalDateTime.now();
        j.processedRows = 0L;
        j.outputRows = 0L;
        return j;
    }

    // getters & setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public Long getFileId() { return fileId; }
    public void setFileId(Long fileId) { this.fileId = fileId; }
    public JobStatus getStatus() { return status; }
    public void setStatus(JobStatus status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public Long getProcessedRows() { return processedRows; }
    public void setProcessedRows(Long processedRows) { this.processedRows = processedRows; }
    public Long getOutputRows() { return outputRows; }
    public void setOutputRows(Long outputRows) { this.outputRows = outputRows; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
