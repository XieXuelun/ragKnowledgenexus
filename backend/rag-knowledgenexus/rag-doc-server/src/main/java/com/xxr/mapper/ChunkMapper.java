package com.xxr.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xxr.document.pojo.DocChunk;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 文档分片Mapper
 */
@Mapper
public interface ChunkMapper extends BaseMapper<DocChunk> {

    /**
     * 删除文档的所有分片
     */
    void deleteByDocId(@Param("docId") Long docId);


    /**
     * 根据文档ID查询分片
     */
    List<DocChunk> selectByDocId(@Param("docId") Long docId);

}
