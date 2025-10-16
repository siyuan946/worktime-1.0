package com.example.worktime.repository;

import com.example.worktime.model.ProcessCode;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ProcessCodeRepository extends JpaRepository<ProcessCode, Long> {
    ProcessCode findByName(String name);
    ProcessCode findByCode(String code);
    List<ProcessCode> findByCodeContainingIgnoreCaseOrNameContainingIgnoreCase(String codePart, String namePart);
}
