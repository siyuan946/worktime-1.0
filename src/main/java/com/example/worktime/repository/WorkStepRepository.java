package com.example.worktime.repository;

import com.example.worktime.model.WorkStep;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkStepRepository extends JpaRepository<WorkStep, Long> {
}
