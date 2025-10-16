package com.example.worktime.repository;

import com.example.worktime.model.Worker;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface WorkerRepository extends JpaRepository<Worker, Long> {
    Worker findByCode(String code);
    List<Worker> findByCodeContainingIgnoreCaseOrNameContainingIgnoreCase(String codePart, String namePart);
}
