package com.xxr.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xxr.common.dtos.PageResponseResult;
import com.xxr.common.dtos.ResponseResult;
import com.xxr.common.enums.AppHttpCodeEnum;
import com.xxr.constant.DeleteConstants;
import com.xxr.mapper.UserMapper;
import com.xxr.service.PermissionService;
import com.xxr.service.UserService;
import com.xxr.user.dtos.UserQueryDTO;
import com.xxr.user.pojo.User;
import com.xxr.user.vos.UserListItemVO;
import com.xxr.user.vos.UserVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestMapping;

@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {
    
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private PermissionService permissionService;
    
    /**
     * 获取用户列表
     *
     * @param userQueryDTO
     * @return
     */
    @Override
    public ResponseResult pageQuery(UserQueryDTO userQueryDTO) {
        log.info("开始处理用户列表查询: {}", userQueryDTO);
        //校验用户权限
        if (!permissionService.isManager()) {
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID, "角色参数非法");
        }
        //校验参数
        if(userQueryDTO==null){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID,"数据不存在");
        }
        // 2. 分页参数 + 合法性校验
        long pageNum = userQueryDTO.getPage();
        long pageSize = userQueryDTO.getPageSize();
        if (pageNum < 1 || pageSize < 1 || pageSize > 100) {
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID, "分页参数非法");
        }
        
        // 使用关联查询（包含部门名称）
        Page<UserListItemVO> page = new Page<>(pageNum, pageSize);
        IPage<UserListItemVO> resultPage = userMapper.selectUserPageWithDept(
                page,
                userQueryDTO.getStatus(),
                userQueryDTO.getRole(),
                userQueryDTO.getDeptId(),
                userQueryDTO.getKeyword()
        );
        
        ResponseResult responseResult =
                new PageResponseResult(userQueryDTO.getPage(), userQueryDTO.getPageSize(), (int)resultPage.getTotal());
        responseResult.setData(resultPage.getRecords());
        return responseResult;
    }

    /**
     * 删除用户
     *
     * @param id
     * @return
     */
    @Override
    public ResponseResult deleteUser(Long id) {
        log.info("开始删除用户: {}", id);
        //校验参数
        if(id==null){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID,"用户数据为空");
        }
        //校验用户是否存在
        User user = getById(id);
        if(user==null){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID,"用户不存在");
        }
        if (!permissionService.isManager()) return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID, "角色参数非法");
        //删除用户(逻辑删除)
        user.setIsDeleted(DeleteConstants.DELETED);
        updateById(user);
        //返回成功
        return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);

    }


    /**
     * 设置用户状态
     * @param user
     * @return
     */
    @Override
    public ResponseResult setStatus(User user) {
        log.info("开始设置用户状态: {}", user);
        if(user==null||user.getStatus()==null){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID,"用户数据为空");
        }
        if (!permissionService.isManager()) return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID, "角色参数非法");
        User dbuser=new User();
        dbuser.setId(user.getId());
        dbuser.setStatus(user.getStatus());
        updateById(dbuser);
        return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);
    }
}
