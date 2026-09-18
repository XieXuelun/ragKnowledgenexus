package com.xxr.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.xxr.common.dtos.PageResponseResult;
import com.xxr.common.dtos.ResponseResult;
import com.xxr.common.enums.AppHttpCodeEnum;
import com.xxr.constant.DeleteConstants;
import com.xxr.enums.VisibleScopeEnum;
import com.xxr.kb.dtos.DocImportDTO;
import com.xxr.kb.dtos.KnowledgeBaseQueryDTO;
import com.xxr.kb.dtos.KnowledgeBaseRequest;
import com.xxr.kb.pojo.DocKnowledgeBase;
import com.xxr.kb.pojo.KbFavorite;
import com.xxr.mapper.KbFavoriteMapper;
import com.xxr.mapper.KnowledgeMapper;
import com.xxr.mapper.UserMapper;
import com.xxr.service.KbFavoriteService;
import com.xxr.service.KnowledgeService;
import com.xxr.service.PermissionService;
import com.xxr.user.pojo.User;
import com.xxr.utils.CurrentUserUtil;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class KnowledgeServiceImpl extends ServiceImpl<KnowledgeMapper, DocKnowledgeBase> implements KnowledgeService {
    @Autowired
    private KbFavoriteMapper kbFavoriteMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private KnowledgeMapper knowledgeMapper;
    @Autowired
    private KbFavoriteService kbFavoriteService;
    @Autowired
    private CurrentUserUtil currentUserUtil;
    @Autowired
    private PermissionService permissionService;



    /**
     * 获取知识库列表
     *
     * @param knowledgeBaseQueryDTO
     * @return
     */
    @Override
    public ResponseResult getList(KnowledgeBaseQueryDTO knowledgeBaseQueryDTO) {
        // 1.参数校验
        if (knowledgeBaseQueryDTO == null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }
        Integer pageNum = knowledgeBaseQueryDTO.getPage();
        Integer pageSize = knowledgeBaseQueryDTO.getPageSize();
        if (pageNum < 1 || pageSize < 1 || pageSize > 100) {
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID, "分页参数非法");
        }

        // 3.封装查询参数
        Map<String, Object> params = new HashMap<>();
        params.put("keyword", knowledgeBaseQueryDTO.getKeyword());
        Long currentId = currentUserUtil.getCurrentId();
        params.put("currentId", currentId);
        // 判断是否为管理员（未登录时视为普通用户，但允许查看公开知识库）
        boolean admin = permissionService.isManager(currentId);
        params.put("isAdmin", admin);
        // 如果用户未登录，只查询公开知识库
        if (currentId == null) {
            params.put("onlyPublic", true);
        }
        // 2. 开始分页（自动拦截后续查询）
        IPage<DocKnowledgeBase> page = new Page<>(pageNum, pageSize);
        // 4.调用 XML 查询
        IPage<DocKnowledgeBase> pageInfo = knowledgeMapper.selectKnowledgeBaseList(page, params);
        // 5.封装分页结果
        ResponseResult responseResult = new PageResponseResult(pageNum, pageSize, (int) page.getTotal());
        responseResult.setData(pageInfo.getRecords());
        return responseResult;
    }

    /**
     * c查询知识库详情
     *
     * @param id
     * @return
     */
    @Override
    public ResponseResult selectKnowledge(Long id) {
        if(id==null){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID,"请求数据为空");
        }
        DocKnowledgeBase knowledgeBase = getById(id);
        ResponseResult responseResult = new ResponseResult();
        responseResult.setData(knowledgeBase);
        return responseResult;
    }

    /**
     * 创建个人知识库
     *
     * @param knowledgeBaseRequest
     * @return
     */
    @Override
    public ResponseResult createKnowledge(KnowledgeBaseRequest knowledgeBaseRequest) {
        //校验参数
        if(knowledgeBaseRequest==null){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }
        Long currentId = currentUserUtil.getCurrentId();
        if(currentId==null){
            return ResponseResult.errorResult(AppHttpCodeEnum.NEED_LOGIN,"请先登录");
        }
        String name = knowledgeBaseRequest.getName();
        Long count = knowledgeMapper.selectCount
                (new LambdaQueryWrapper<DocKnowledgeBase>().eq(DocKnowledgeBase::getName, name));
        if(count>0){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID,"知识库名称已存在");
        }
        //补全属性
        DocKnowledgeBase knowledgeBase = new DocKnowledgeBase();
        BeanUtils.copyProperties(knowledgeBaseRequest,knowledgeBase);
        knowledgeBase.setOwnerId(currentId);
        //补全部门id的属性
        User user = userMapper.selectById(currentId);
        if(user!=null){
            knowledgeBase.setDeptId(user.getDeptId());
        }
        knowledgeBase.setId(null);
        knowledgeBase.setIsDeleted(DeleteConstants.NOT_DELETED);
        knowledgeBase.setCreateTime(LocalDateTime.now());
        //保存数据
        save(knowledgeBase);
        return ResponseResult.okResult(knowledgeBase);
    }

    /**
     * 修改知识库
     *
     * @param knowledgeBaseRequest
     * @return
     */
    @Override
    public ResponseResult updateKnowledge(KnowledgeBaseRequest knowledgeBaseRequest) {
        //校验参数
        if(knowledgeBaseRequest==null){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }
        //检查用户id
        Long currentId = currentUserUtil.getCurrentId();
        if(currentId==null){
            return ResponseResult.errorResult(AppHttpCodeEnum.NEED_LOGIN,"请先登录");
        }
        //检查知识库是否存在
        DocKnowledgeBase knowledgeBase = getById(knowledgeBaseRequest.getId());
        if(knowledgeBase==null){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID,"知识库不存在");
        }
        //检查知识库是否属于当前用户
        if(!(knowledgeBase.getOwnerId().equals(currentId))){
            return ResponseResult.errorResult(AppHttpCodeEnum.NO_OPERATOR_AUTH,"无法更改不属于您的知识库");
        }
        //更新知识库信息
        DocKnowledgeBase knowledge = new DocKnowledgeBase();
        BeanUtils.copyProperties(knowledgeBaseRequest,knowledge);//更新知识库信息
        knowledge.setUpdateTime(LocalDateTime.now());
        updateById(knowledge);
        return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);
    }

    @Override
    public ResponseResult favoriteKnowledge(Long kbId) {
        //校验参数
        if(kbId==null){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }
        //检查登录状态
        Long currentId = currentUserUtil.getCurrentId();
        if(currentId==null){
            return ResponseResult.errorResult(AppHttpCodeEnum.NEED_LOGIN,"请先登录");
        }
        //检查知识库是否存在
        DocKnowledgeBase knowledgeBase = getById(kbId);
        if(knowledgeBase == null){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID, "知识库不存在");
        }
        //检查该用户是否已经收藏了此知识库
        KbFavorite kbFavorite = kbFavoriteMapper.selectOne(Wrappers.<KbFavorite>lambdaQuery()
                .eq(KbFavorite::getKbId, kbId)
                .eq(KbFavorite::getUserId, currentId)
                .orderByDesc(KbFavorite::getCreateTime)); // 获取最新的记录
        
        if(kbFavorite == null || kbFavorite.getIsDeleted().equals(DeleteConstants.DELETED)) {
            // 之前未收藏或已取消收藏，现在要收藏
            if(kbFavorite == null) {
                // 完全新建收藏记录
                kbFavorite = new KbFavorite();
                kbFavorite.setKbId(kbId);
                kbFavorite.setUserId(currentId);
                kbFavorite.setCreateTime(LocalDateTime.now());
                kbFavoriteMapper.insert(kbFavorite);
            } else {
                // 更新已存在的记录为未删除状态（重新收藏）
                kbFavorite.setIsDeleted(DeleteConstants.NOT_DELETED);
                kbFavoriteMapper.updateById(kbFavorite);
            }
            return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);
        } else {
            // 已经收藏，现在要取消收藏
            kbFavorite.setIsDeleted(DeleteConstants.DELETED);
            kbFavoriteMapper.updateById(kbFavorite);
            return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);
        }
    }

    @Override
    public ResponseResult deleteKnowledge(Long kbId) {
        if (kbId == null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID, "知识库ID不能为空");
        }

        Long currentId = currentUserUtil.getCurrentId();
        if (currentId == null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.NEED_LOGIN, "请先登录");
        }

        DocKnowledgeBase knowledgeBase = getById(kbId);
        if (knowledgeBase == null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID, "知识库不存在");
        }

        if (!knowledgeBase.getOwnerId().equals(currentId)) {
            return ResponseResult.errorResult(AppHttpCodeEnum.NO_OPERATOR_AUTH, "只能删除自己创建的知识库");
        }
        knowledgeBase.setIsDeleted(DeleteConstants.DELETED);
        updateById(knowledgeBase);
      /*
        LambdaQueryWrapper<KbFavorite> favoriteWrapper = new LambdaQueryWrapper<>();
        favoriteWrapper.eq(KbFavorite::getKbId, kbId);
        kbFavoriteMapper.delete(favoriteWrapper);
        */
        //删除知识库点赞表数据
        kbFavoriteService.lambdaUpdate().eq(KbFavorite::getKbId, kbId).set(KbFavorite::getIsDeleted, DeleteConstants.DELETED);
        return ResponseResult.okResult("删除成功");
    }

    /**
     * 查询知识库名称
     *
     * @return
     */
    @Override
    public ResponseResult selectKnowledgeName() {
       List<String> knowledgeNames = knowledgeMapper.selectName();
       ResponseResult responseResult = new ResponseResult();
       responseResult.setData(knowledgeNames);
        return responseResult;
    }


}
