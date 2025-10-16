package com.example.worktime.model;

import javax.persistence.*;
import java.time.LocalDateTime;

import com.example.worktime.model.UploadedFile;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Entity representing a single work record parsed from Excel uploads.
 */

@Entity
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class WorkRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 产品代码 -> 通知单号
    private String notificationNumber;

    // 所属产品 -> 产品名称
    private String productName;

    // 代号 -> 图号
    private String drawingNumber;

    // 产品代号
    private String productCode;

    // 名称
    private String partName;

    // 计划数
    private Integer planQty;

    // 原始Excel行号
    private Integer sourceRowNumber;

    // 工序名称
    private String processName;

    // 工序代码(由工序名称映射)
    private String processCode;

    // 条形码
    private String barcode;

    // 条形码图片
    @Lob
    private byte[] barcodeImage;

    // 批次号
    private String batchNumber;

    @Transient
    private Boolean hoursMissing;

    @Transient
    private Boolean codeMissing;

    // 单件工时
    private Double hours;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "file_id")
    @com.fasterxml.jackson.annotation.JsonIgnore
    private UploadedFile file;

    // 是否补录
    private Boolean supplemental;

    // 人员代码 (多人员用逗号分隔)
    private String workerCodes;

    // 人员姓名 (回显)
    private String workerNames;

    // 每个员工完成的数量，用空格分隔
    private String workerQtys;

    // 合格数量
    @javax.persistence.Column(precision = 10, scale = 2)
    private Double qualifiedQty;

    // 单件工时小计 = 合格数量 * 单件工时
    private Double hourSubtotal;

    // 是否已填写合格数
    private Boolean filled;

    // 冗余：所属自然月（YYYY-MM）
    private String naturalMonth;

    private LocalDateTime startTime;
    private LocalDateTime endTime;

    // 检验员
    private String inspector;

    private String remark1;
    private String remark2;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNotificationNumber() { return notificationNumber; }
    public void setNotificationNumber(String notificationNumber) { this.notificationNumber = notificationNumber; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public String getDrawingNumber() { return drawingNumber; }
    public void setDrawingNumber(String drawingNumber) { this.drawingNumber = drawingNumber; }

    public String getProductCode() { return productCode; }
    public void setProductCode(String productCode) { this.productCode = productCode; }

    public String getPartName() { return partName; }
    public void setPartName(String partName) { this.partName = partName; }

    public Integer getPlanQty() { return planQty; }
    public void setPlanQty(Integer planQty) { this.planQty = planQty; }

    public Integer getSourceRowNumber() { return sourceRowNumber; }
    public void setSourceRowNumber(Integer sourceRowNumber) { this.sourceRowNumber = sourceRowNumber; }

    public String getProcessName() { return processName; }
    public void setProcessName(String processName) { this.processName = processName; }

    public String getProcessCode() { return processCode; }
    public void setProcessCode(String processCode) { this.processCode = processCode; }

    public String getBarcode() { return barcode; }
    public void setBarcode(String barcode) { this.barcode = barcode; }

    public byte[] getBarcodeImage() { return barcodeImage; }
    public void setBarcodeImage(byte[] barcodeImage) { this.barcodeImage = barcodeImage; }

    public String getBatchNumber() { return batchNumber; }
    public void setBatchNumber(String batchNumber) { this.batchNumber = batchNumber; }

    public Boolean getHoursMissing() { return hoursMissing; }
    public void setHoursMissing(Boolean hoursMissing) { this.hoursMissing = hoursMissing; }

    public Boolean getCodeMissing() { return codeMissing; }
    public void setCodeMissing(Boolean codeMissing) { this.codeMissing = codeMissing; }

    public Double getHours() { return hours; }
    public void setHours(Double hours) { this.hours = hours; }

    @com.fasterxml.jackson.annotation.JsonIgnore
    public UploadedFile getFile() { return file; }
    public void setFile(UploadedFile file) { this.file = file; }

    public Boolean getSupplemental() { return supplemental; }
    public void setSupplemental(Boolean supplemental) { this.supplemental = supplemental; }

    public String getWorkerCodes() { return workerCodes; }
    public void setWorkerCodes(String workerCodes) { this.workerCodes = workerCodes; }

    public String getWorkerNames() { return workerNames; }
    public void setWorkerNames(String workerNames) { this.workerNames = workerNames; }

    public String getWorkerQtys() { return workerQtys; }
    public void setWorkerQtys(String workerQtys) { this.workerQtys = workerQtys; }

    public Double getQualifiedQty() { return qualifiedQty; }
    public void setQualifiedQty(Double qualifiedQty) { this.qualifiedQty = qualifiedQty; }

    public Double getHourSubtotal() { return hourSubtotal; }
    public void setHourSubtotal(Double hourSubtotal) { this.hourSubtotal = hourSubtotal; }

    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }

    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }

    public String getInspector() { return inspector; }
    public void setInspector(String inspector) { this.inspector = inspector; }

    public String getRemark1() { return remark1; }
    public void setRemark1(String remark1) { this.remark1 = remark1; }

    public String getRemark2() { return remark2; }
    public void setRemark2(String remark2) { this.remark2 = remark2; }

    public Boolean getFilled() { return filled; }
    public void setFilled(Boolean filled) { this.filled = filled; }

    public String getNaturalMonth() { return naturalMonth; }
    public void setNaturalMonth(String naturalMonth) { this.naturalMonth = naturalMonth; }
}