package com.example.worktime.model;

import javax.persistence.*;
import java.time.LocalDate;

/**
 * Worker entity representing an employee code record.
 */
@Entity
public class Worker {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 工号
    private String code;

    // 姓名
    private String name;

    // 车间
    private String workshop;

    // 班组
    private String team;

    // 入厂日期
    private LocalDate entryDate;

    // 离厂日期
    private LocalDate leaveDate;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getWorkshop() { return workshop; }
    public void setWorkshop(String workshop) { this.workshop = workshop; }

    public String getTeam() { return team; }
    public void setTeam(String team) { this.team = team; }

    public LocalDate getEntryDate() { return entryDate; }
    public void setEntryDate(LocalDate entryDate) { this.entryDate = entryDate; }

    public LocalDate getLeaveDate() { return leaveDate; }
    public void setLeaveDate(LocalDate leaveDate) { this.leaveDate = leaveDate; }
}
