package com.example.worktime.repository;

import com.example.worktime.model.ImportJob;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImportJobRepository extends JpaRepository<ImportJob, String> { }
