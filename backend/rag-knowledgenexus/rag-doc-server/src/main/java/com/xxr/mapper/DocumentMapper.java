package com.xxr.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.xxr.document.dtos.DocumentQueryDto;
import com.xxr.document.pojo.DocDocument;
import com.xxr.document.vos.DocDocumentVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface DocumentMapper extends BaseMapper<DocDocument> {
    IPage<DocDocumentVO> selectList(IPage<DocDocumentVO> page, @Param("map") Map<String,Object> map);
    
    /**
     * 根据知识库ID和状态查询文档
     */
    List<DocDocument> selectByKbIdAndStatus(Long kbId, Integer status);
    
    /**
     * 根据状态查询文档
     */
    List<DocDocument> selectByStatus(Integer status);

    List<DocDocument> selectbyKbId(Long kbId,boolean admin,Long currentId);
}
