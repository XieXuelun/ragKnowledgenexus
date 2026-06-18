package com.xxr.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.xxr.dept.pojo.Department;
import com.xxr.kb.pojo.DocKnowledgeBase;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface KnowledgeMapper  extends BaseMapper<DocKnowledgeBase> {
    IPage<DocKnowledgeBase> selectKnowledgeBaseList(IPage<DocKnowledgeBase> page, @Param("params") Map<String, Object> params);
    @Select("select name from doc_knowledge_base where is_deleted = 0")
    List<String> selectName();

    /**
     * 查询用户有权访问的知识库 ID 列表
     * @param userId 当前用户 ID
     * @return 可访问的知识库 ID 列表
     */
    List<Long> selectAccessibleKbIds(@Param("userId") Long userId);
}
