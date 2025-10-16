package com.example.worktime.repository;

import com.example.worktime.model.WorkRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WorkRecordRepository extends JpaRepository<WorkRecord, Long> {
    java.util.List<WorkRecord> findByBarcode(String barcode);

    java.util.List<WorkRecord> findByFileId(Long fileId);

    java.util.List<WorkRecord> findByFileIdAndFilledTrue(Long fileId);

    java.util.List<WorkRecord> findByDrawingNumber(String drawingNumber);

    @Query("select r from WorkRecord r where r.file.uploadTime >= :start and r.file.uploadTime < :end and r.filled = true")
    java.util.List<WorkRecord> findByUploadDate(@Param("start") java.time.LocalDateTime start,
                                                @Param("end") java.time.LocalDateTime end);

    java.util.List<WorkRecord> findByFilledTrue();

    void deleteByFileId(Long fileId);

    long countByFileId(Long fileId);

    Page<WorkRecord> findByFileIdAndDrawingNumber(Long fileId, String drawingNumber, Pageable pageable);

    @Query("select r.drawingNumber as drawing, count(r) as cnt, min(r.sourceRowNumber) as minRow from WorkRecord r where r.file.id = :fileId group by r.drawingNumber order by r.drawingNumber")
    java.util.List<Object[]> findDrawingBuckets(@Param("fileId") Long fileId);

    Page<WorkRecord> findByNaturalMonthAndFilledTrue(String naturalMonth, Pageable pageable);

    java.util.List<WorkRecord> findByNaturalMonthAndFilledTrue(String naturalMonth);
}