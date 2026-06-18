package com.xxr.dept.dtos;

import lombok.Data;

import java.util.List;

@Data
public class DepartmentTreeDTO {
    
    private Long id;
    
    private String name;
    
    private Long parentId;
    
    private Integer sort;
    
    private String remark;
    
    private Integer userCount;
    
    private List<DepartmentTreeDTO> children;
}
