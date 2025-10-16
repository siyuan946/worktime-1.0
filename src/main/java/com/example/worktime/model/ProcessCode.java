package com.example.worktime.model;

import javax.persistence.*;

/**
 * Entity for process code mapping.
 */
@Entity
public class ProcessCode {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 代号
    private String code;

    // 工序名称
    private String name;

    // 大类
    private String category;

    // 包含工作内容
    private String content;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
}
