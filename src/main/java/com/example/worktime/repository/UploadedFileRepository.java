package com.example.worktime.repository;

import com.example.worktime.model.UploadedFile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UploadedFileRepository extends JpaRepository<UploadedFile, Long> {
    java.util.Optional<UploadedFile> findByFileName(String fileName);

    @org.springframework.data.jpa.repository.Query("select f from UploadedFile f where exists (select 1 from WorkRecord w where w.file = f)")
    java.util.List<UploadedFile> findAllWithRecords();
}
