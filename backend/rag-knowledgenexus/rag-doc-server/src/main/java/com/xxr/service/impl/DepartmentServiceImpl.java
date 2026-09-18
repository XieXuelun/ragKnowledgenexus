package com.xxr.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xxr.common.dtos.ResponseResult;
import com.xxr.common.enums.AppHttpCodeEnum;
import com.xxr.constant.DeleteConstants;
import com.xxr.dept.dtos.DepartmentTreeDTO;
import com.xxr.dept.pojo.Department;
import com.xxr.mapper.DepartmentMapper;
import com.xxr.mapper.UserMapper;
import com.xxr.service.DepartmentService;
import com.xxr.service.PermissionService;
import com.xxr.user.pojo.User;
import com.xxr.user.vos.UserListItemVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DepartmentServiceImpl extends ServiceImpl<DepartmentMapper, Department> implements DepartmentService {

    @Autowired
    private UserMapper userMapper;
    @Autowired
    private PermissionService permissionService;


    /**
     * 将Department转换为DepartmentTreeDTO并计算成员数量
     */
    
    @Override
    public List<DepartmentTreeDTO> getTree(Long parentId) {
        List<Department> departments = list(new LambdaQueryWrapper<Department>()
                .orderByAsc(Department::getSort));
        if (departments.isEmpty()) return java.util.Collections.emptyList();

        Set<Long> allIds = departments.stream().map(Department::getId).collect(java.util.stream.Collectors.toSet());

        if (parentId == null) {
            // Build full tree: find roots and build child map
            java.util.List<Department> roots = new java.util.ArrayList<>();
            for (Department d : departments) {
                Long pid = d.getParentId();
                if (pid == null || pid == 0L || pid.equals(d.getId()) || !allIds.contains(pid)) {
                    roots.add(d);
                }
            }
            java.util.Map<Long, java.util.List<Department>> childMap = new java.util.HashMap<>();
            for (Department d : departments) {
                Long pid = d.getParentId();
                if (pid != null && !pid.equals(d.getId()) && allIds.contains(pid)) {
                    childMap.computeIfAbsent(pid, k -> new java.util.ArrayList<>()).add(d);
                }
            }
            return buildTree(roots, childMap);
        }

        // parentId specified: build from that parent's children
        return buildTreeIterative(departments, parentId, allIds);
    }

    private List<DepartmentTreeDTO> buildTree(
            List<Department> roots,
            Map<Long, java.util.List<Department>> childMap) {
            List<DepartmentTreeDTO> result = new java.util.ArrayList<>();
            Set<Long> visited = new java.util.HashSet<>();
            Queue<java.util.Map.Entry<Department, DepartmentTreeDTO>> queue = new LinkedList<>();

        for (Department root : roots) {
            if (visited.contains(root.getId())) continue;
            visited.add(root.getId());
            DepartmentTreeDTO dto = convertToTreeDTO(root);
            result.add(dto);
            queue.offer(new java.util.AbstractMap.SimpleEntry<>(root, dto));
        }

        while (!queue.isEmpty()) {
            java.util.Map.Entry<Department, DepartmentTreeDTO> entry = queue.poll();
            Department parent = entry.getKey();
            DepartmentTreeDTO parentDTO = entry.getValue();
            java.util.List<Department> children = childMap.getOrDefault(parent.getId(), java.util.Collections.emptyList());

            if (!children.isEmpty()) {
                java.util.List<DepartmentTreeDTO> childDTOs = new java.util.ArrayList<>();
                for (Department child : children) {
                    if (!visited.contains(child.getId())) {
                        visited.add(child.getId());
                        DepartmentTreeDTO childDTO = convertToTreeDTO(child);
                        childDTOs.add(childDTO);
                        queue.offer(new java.util.AbstractMap.SimpleEntry<>(child, childDTO));
                    }
                }
                if (!childDTOs.isEmpty()) parentDTO.setChildren(childDTOs);
            }
        }
        return result;
    }

    private java.util.List<DepartmentTreeDTO> buildTreeIterative(
            java.util.List<Department> departments, Long rootParentId, java.util.Set<Long> allIds) {
        java.util.Map<Long, java.util.List<Department>> childMap = new java.util.HashMap<>();
        for (Department dept : departments) {
            Long pId = dept.getParentId();
            if (pId == null) continue;
            if (pId.equals(dept.getId())) continue;
            if (pId != rootParentId && !allIds.contains(pId)) continue;
            childMap.computeIfAbsent(pId, k -> new java.util.ArrayList<>()).add(dept);
        }

        java.util.List<DepartmentTreeDTO> result = new java.util.ArrayList<>();
        java.util.Set<Long> visited = new java.util.HashSet<>();
        java.util.Queue<java.util.Map.Entry<Department, DepartmentTreeDTO>> queue = new java.util.LinkedList<>();
        int maxIter = departments.size() * 2, iter = 0;

        java.util.List<Department> rootDepts = childMap.getOrDefault(rootParentId, java.util.Collections.emptyList());
        for (Department dept : rootDepts) {
            if (!visited.contains(dept.getId())) {
                visited.add(dept.getId());
                DepartmentTreeDTO dto = convertToTreeDTO(dept);
                result.add(dto);
                queue.offer(new java.util.AbstractMap.SimpleEntry<>(dept, dto));
            }
        }

        while (!queue.isEmpty() && iter < maxIter) {
            iter++;
            java.util.Map.Entry<Department, DepartmentTreeDTO> entry = queue.poll();
            Department parentDept = entry.getKey();
            DepartmentTreeDTO parentTreeDTO = entry.getValue();
            java.util.List<Department> children = childMap.getOrDefault(parentDept.getId(), java.util.Collections.emptyList());

            if (!children.isEmpty()) {
                java.util.List<DepartmentTreeDTO> childDTOs = new java.util.ArrayList<>();
                for (Department child : children) {
                    if (!visited.contains(child.getId())) {
                        visited.add(child.getId());
                        DepartmentTreeDTO childDTO = convertToTreeDTO(child);
                        childDTOs.add(childDTO);
                        queue.offer(new java.util.AbstractMap.SimpleEntry<>(child, childDTO));
                    }
                }
                if (!childDTOs.isEmpty()) parentTreeDTO.setChildren(childDTOs);
            }
        }
        return result;
    }

    private DepartmentTreeDTO convertToTreeDTO(Department dept) {
        DepartmentTreeDTO treeDTO = new DepartmentTreeDTO();
        org.springframework.beans.BeanUtils.copyProperties(dept, treeDTO);
        long memberCount = userMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.xxr.user.pojo.User>()
                        .eq(com.xxr.user.pojo.User::getDeptId, dept.getId())
                        .eq(com.xxr.user.pojo.User::getIsDeleted, 0)
        );
        treeDTO.setUserCount((int) memberCount);
        return treeDTO;
    }



    @Override
    public List<Department> getList() {
        return list(new LambdaQueryWrapper<Department>()
                .orderByAsc(Department::getSort));
    }

    /**
     * 获取部门员工
     * @param deptId
     * @return
     */
    @Override
    public List<UserListItemVO> getMembers(Long deptId) {
        //校验参数
        if (deptId == null) {
            throw new IllegalArgumentException("部门ID不能为空");
        }
        List<User> users = userMapper.selectList
                (new LambdaQueryWrapper<User>()
                .eq(User::getDeptId, deptId)
                .eq(User::getIsDeleted, 0)
                .orderByAsc(User::getId));
        
        return users.stream()
                .map(user -> {
                    UserListItemVO vo = new UserListItemVO();
                    BeanUtils.copyProperties(user, vo);
                    return vo;
                })
                .collect(Collectors.toList());
    }

    /**
     * 创建部门
     */
    @Override
    public ResponseResult create(Department department) {
        // 校验管理员权限
        ResponseResult checkResult = permissionService.checkSuperAdminPermission();
        if (checkResult != null) {
            return checkResult;
        }
        
        if (department == null || !StringUtils.hasText(department.getName())) {
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID, "部门名称不能为空");
        }
        department.setId(null); // let MP generate snowflake ID
        department.setCreateTime(LocalDateTime.now());
        department.setUpdateTime(LocalDateTime.now());
        department.setIsDeleted(DeleteConstants.NOT_DELETED);
        save(department);
        return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);
    }

    /**
     * 更新部门
     */
    @Override
    public ResponseResult update(Department department) {
        // 校验管理员权限
        ResponseResult checkResult = permissionService.checkSuperAdminPermission();
        if (checkResult != null) {
            return checkResult;
        }
        
        if (department == null || department.getId() == null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID, "部门ID不能为空");
        }
        if (!StringUtils.hasText(department.getName())) {
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID, "部门名称不能为空");
        }
        department.setUpdateTime(LocalDateTime.now());
        updateById(department);
        return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);
    }

    /**
     * 删除部门
     */
    @Override
    public ResponseResult delete(Long id) {
        // 校验管理员权限
        ResponseResult checkResult = permissionService.checkSuperAdminPermission();
        if (checkResult != null) {
            return checkResult;
        }
        
        if (id == null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID, "部门ID不能为空");
        }
        // 检查是否有子部门
        long childCount = count(new LambdaQueryWrapper<Department>().eq(Department::getParentId, id));
        if (childCount > 0) {
            return ResponseResult.errorResult(AppHttpCodeEnum.DATA_EXIST, "该部门存在子部门，无法删除");
        }
        // 检查是否有成员
        long userCount = userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getDeptId, id));
        if (userCount > 0) {
            return ResponseResult.errorResult(AppHttpCodeEnum.DATA_EXIST, "该部门存在成员，无法删除");
        }
        removeById(id);
        return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);
    }


}
