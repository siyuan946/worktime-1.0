package com.example.worktime.model;

import javax.persistence.*;
import java.time.LocalDate;
import java.util.List;

@Entity
public class WorkTime {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 代号
    private String code;

    // 名称
    private String name;

    // 投产日期
    private LocalDate startDate;

    // 完成日期
    private LocalDate endDate;

    @OneToMany(mappedBy = "workTime", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<WorkStep> steps;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public List<WorkStep> getSteps() { return steps; }
    public void setSteps(List<WorkStep> steps) { this.steps = steps; }
}
