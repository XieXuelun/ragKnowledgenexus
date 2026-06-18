package com.xxr.controller.v1;

import com.xxr.common.dtos.ResponseResult;
import com.xxr.dept.dtos.DepartmentTreeDTO;
import com.xxr.dept.pojo.Department;
import com.xxr.service.DepartmentService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/department")
@Api(tags = "部门管理接口")
public class DepartmentController {
    
    @Autowired
    private DepartmentService departmentService;

    /**
     * 获取部门树
     * 获取完整的部门树形结构（所有人都可以查看）
     * @param parentId 父部门ID，不传则默认从根部门开始
     * @return
     */
    @GetMapping("/tree")
    @ApiOperation(value = "获取部门树")
    public ResponseResult<List<DepartmentTreeDTO>> getTree(
            @RequestParam(value = "parentId", required = false) Long parentId) {
        List<DepartmentTreeDTO> tree = departmentService.getTree(parentId);
        ResponseResult responseResult = new ResponseResult();
        responseResult.setData(tree);
        return responseResult;
    }

    /**
     * 获取部门列表
     * 获取部门平铺列表（不含树形结构）
     * @return
     */
    @GetMapping("/list")
    @ApiOperation(value = "获取部门列表")
    public ResponseResult<List<Department>> getList() {
        List<Department> list = departmentService.getList();
        return ResponseResult.okResult(list);
    }

    /**
     * 获取部门成员
     * 获取部门下的所有成员列表
     * @param deptId 部门ID
     * @return
     */
    @GetMapping("/{deptId}/members")
    @ApiOperation(value = "获取部门成员")
    public ResponseResult getMembers(@PathVariable Long deptId) {
        return ResponseResult.okResult(departmentService.getMembers(deptId));
    }

    /**
     * 创建部门（管理员）
     */
    @PostMapping("/create")
    @ApiOperation(value = "创建部门")
    public ResponseResult create(@RequestBody Department department) {
        return departmentService.create(department);
    }

    /**
     * 更新部门（管理员）
     */
    @PostMapping("/update")
    @ApiOperation(value = "更新部门")
    public ResponseResult update(@RequestBody Department department) {
        return departmentService.update(department);
    }

    /**
     * 删除部门（管理员）
     */
    @DeleteMapping("/delete/{id}")
    @ApiOperation(value = "删除部门")
    public ResponseResult delete(@PathVariable Long id) {
        return departmentService.delete(id);
    }
}