package com.xxr.service;

import com.xxr.common.dtos.ResponseResult;
import com.xxr.dept.dtos.DepartmentTreeDTO;
import com.xxr.dept.pojo.Department;
import com.xxr.user.dtos.UserListItemDTO;
import com.xxr.user.vos.UserListItemVO;

import java.util.List;

public interface DepartmentService {
    

    
    List<Department> getList();
    
    List<UserListItemVO> getMembers(Long deptId);
    
    ResponseResult create(Department department);
    
    ResponseResult update(Department department);
    
    ResponseResult delete(Long id);

    List<DepartmentTreeDTO> getTree(Long parentId);
}
