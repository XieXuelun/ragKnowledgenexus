package com.xxr.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.xxr.common.dtos.PageResponseResult;
import com.xxr.common.dtos.ResponseResult;
import com.xxr.common.enums.AppHttpCodeEnum;
import com.xxr.constant.DeleteConstants;
import com.xxr.constant.DocumentParseStatusConstants;
import com.xxr.document.dtos.DocumentQueryDto;
import com.xxr.document.pojo.DocDocument;

import com.xxr.document.vos.DocDocumentVO;
import com.xxr.kb.pojo.DocKnowledgeBase;
import com.xxr.mapper.DocumentMapper;
import com.xxr.service.DocumentParseService;
import com.xxr.service.DocumentService;
import com.xxr.service.PermissionService;
import com.xxr.utils.CurrentUserUtil;
import com.xxr.utils.MinioUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class DocumentServiceImpl extends ServiceImpl<DocumentMapper, DocDocument> implements DocumentService {
    @Autowired
    private DocumentMapper documentMapper;
    @Autowired
    private MinioUtil minioUtil;
    @Autowired
    private CurrentUserUtil currentUserUtil;
    @Autowired
    private PermissionService permissionService;
    @Autowired
    private DocumentParseService documentParseService;
    /**
     * 查看某知识库下的文档列表，只返回当前用户有权访问的文档。
     *校验参数
     * 检查分页参数
     * 获取当前登录用户
     *
     * @param documentQueryDto
     * @return
     */
    @Override
    public ResponseResult getList(DocumentQueryDto documentQueryDto) {
        if(documentQueryDto==null){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }
        Integer pageNum = documentQueryDto.getPage();
        Integer pageSize =documentQueryDto.getPageSize();
        if(pageNum<1||pageSize<1||pageSize>100){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID,"分页参数非法");
        }
        //构建查询条件
        Long userId = currentUserUtil.getCurrentId();
        if(userId==null){
            return ResponseResult.errorResult(AppHttpCodeEnum.NEED_ADMIND,"登录后再操作");
        }
        boolean isAdmin=permissionService.isManager(userId);
        Map<String,Object> map = new HashMap<>();
        map.put("kb_id",documentQueryDto.getKbId());
        map.put("userId",userId)  ;
        map.put("isAdmin",isAdmin);
        map.put("keyword",documentQueryDto.getKeyword());
        map.put("fileType",documentQueryDto.getFileType());
        map.put("parseStatus",documentQueryDto.getParseStatus());
        //分页查询
        IPage<DocDocumentVO> page = new Page<>(pageNum, pageSize);
        // 4.调用 XML 查询
        IPage<DocDocumentVO> pageInfo = documentMapper.selectList(page, map);
        ResponseResult responseResult = new PageResponseResult(pageNum, pageSize, (int) page.getTotal());
        responseResult.setData(pageInfo.getRecords());
        return responseResult;

    }

    /**
     * 上传知识库的文档
     *创建文档
     * @param file
     * @return
     */
    @Override
    public ResponseResult uploadDocument(MultipartFile file, Long kbId) throws Exception {
        //校验参数
        if(file==null){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }
        long size = file.getSize();
        String fileName=file.getOriginalFilename();
        //获取扩展名
        String fileType = fileName.substring(fileName.lastIndexOf(".")+1);
        //上传文档
        String documenturl = minioUtil.uploadDocument(file);
        //持久化存储
        DocDocument document = new DocDocument();
        Long currentId = currentUserUtil.getCurrentId();
        if(currentId==null){
            return ResponseResult.errorResult(AppHttpCodeEnum.NEED_ADMIND);
        }
        document.setUploadUserId(currentId);
        document.setFileSize(size);
        document.setFileType(fileType);
        document.setTitle(fileName.substring(0, fileName.lastIndexOf(".")));
        document.setFileName(fileName);
        document.setKbId(kbId);
        document.setMinioPath(documenturl);//地址
        document.setCreateTime(LocalDateTime.now());
        document.setIsDeleted(DeleteConstants.NOT_DELETED);
        document.setParseStatus(DocumentParseStatusConstants.PENDING); // 0=待处理
        save(document);
        if(document.getId()==null){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID,"文档上传失败");
        }
        // 上传完成后异步调用文档解析
        log.info("文档上传成功，开始异步解析: docId={}, fileName={}", document.getId(), fileName);
        documentParseService.parseDocumentAsync(document);
        
        return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);
    }

    /**
     * 查询文档详情
     *
     * @param id
     * @return
     */
    @Override
    public ResponseResult getDocumentDetail(Long id) {
        if(id==null){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }
        DocDocument docDocument = getById(id);
        ResponseResult responseResult = new ResponseResult();
        responseResult.setData(docDocument);
        return responseResult;
    }

    /**
     * 获取文档预览地址
     *
     * @param docId
     * @param expireSeconds
     * @return
     */
    @Override
    public ResponseResult getDocumentPreviewUrl(Long docId, Integer expireSeconds) throws Exception {
        if(docId==null){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }
        // 默认有效期600秒
        if (expireSeconds == null || expireSeconds <= 0) {
            expireSeconds = 600;
        }
        DocDocument docDocument = getById(docId);
        if(docDocument==null){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID,"文档不存在");
        }
        // 生成预签名URL
        String previewUrl = minioUtil.getPresignedUrl(docDocument.getMinioPath(), expireSeconds);
        // 计算过期时间
        long expireAt = System.currentTimeMillis() + (long) expireSeconds * 1000;
        ResponseResult responseResult = new ResponseResult();
        //封装数据返回
        Map<String,Object> map = new HashMap<>();
        map.put("previewUrl",previewUrl);
        map.put("expireAt",expireAt);
        responseResult.setData(map);
        return responseResult;
    }

    /**
     * 更新文档标题
     *
     * @param docId
     * @param fileName
     * @return
     */
    @Override
    public ResponseResult updateDocumentTitle(Long docId, String fileName) {
        if(docId==null){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID,"文档ID不能为空");
        }
        //校验登录用户是否为该文档的作者
        Long currentId = currentUserUtil.getCurrentId();
        if(currentId==null){
            return ResponseResult.errorResult(AppHttpCodeEnum.NEED_ADMIND,"请先登录后再操作");
        }
        DocDocument docDocument = getById(docId);
        if(docDocument==null){
            return ResponseResult.errorResult(AppHttpCodeEnum.DATA_NOT_EXIST,"文档不存在");
        }
        log.info("当前登录ID:{}",currentId);
        if(!(currentId.equals(docDocument.getUploadUserId()))){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID,"您不是该文档的作者,无权限操作");
        }
        docDocument.setFileName(fileName);
        updateById(docDocument);
        return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);
    }

    /**
     * 删除文档信息，逻辑删除
     *
     * @param docId
     * @return
     */
    @Override
    public ResponseResult deleteDocument(Long docId) {
        if(docId==null){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID,"文档ID不能为空");
        }
        DocDocument docDocument = getById(docId);
        if(docDocument==null){
            return ResponseResult.errorResult(AppHttpCodeEnum.DATA_NOT_EXIST,"文档不存在");
        }
        //校验登录用户是否为该文档的作者
        Long currentId = currentUserUtil.getCurrentId();
        if(currentId==null){
            return ResponseResult.errorResult(AppHttpCodeEnum.NEED_ADMIND,"请先登录后再操作");
        }
        if(!(currentId.equals(docDocument.getUploadUserId()))){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID,"您不是该文档的作者,无权限操作");
        }
        docDocument.setIsDeleted(DeleteConstants.DELETED);
        updateById(docDocument);
        return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);
    }

    /**
     * 重新解析文档（删除旧分片，重新解析）
     *
     * @param docId 文档ID
     * @return 解析结果
     */
    @Override
    public ResponseResult reparseDocument(Long docId) {
        //校验参数
        if(docId==null){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID,"文档ID不能为空");
        }
        //查询文档是否存在
        DocDocument docDocument = getById(docId);
        if(docDocument==null){
            return ResponseResult.errorResult(AppHttpCodeEnum.DATA_NOT_EXIST,"文档不存在");
        }
        //检验登录用户是否为该文档的作者
        Long currentId = currentUserUtil.getCurrentId();
        Long uploadUserId = docDocument.getUploadUserId ();
        if(!(currentId.equals(uploadUserId))){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID,"您不是该文档的作者,无权限操作");
        }
        //异步调用文档解析服务重新解析
        documentParseService.reparseDocument(docDocument);
        return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);
    }

    /**
     * 根据知识库id查询文档
     * @param kbId
     * @return
     */
    @Override
    public ResponseResult selectbyKbId(Long kbId) {
        //校验参数
        if(kbId==null){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }
        Long currentId = currentUserUtil.getCurrentId();
        //查询文档
        boolean admin = permissionService.isManager(currentId);
        List<DocDocument> documents=documentMapper.selectbyKbId(kbId,admin,currentId);
        return ResponseResult.okResult(documents);
    }


}
