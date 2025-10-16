package com.example.worktime.repository;

import com.example.worktime.model.WorkTime;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkTimeRepository extends JpaRepository<WorkTime, Long> {
}
