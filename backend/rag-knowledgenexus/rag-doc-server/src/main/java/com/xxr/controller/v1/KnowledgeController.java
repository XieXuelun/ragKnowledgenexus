package com.xxr.controller.v1;

import com.xxr.common.dtos.ResponseResult;
import com.xxr.kb.dtos.KnowledgeBaseQueryDTO;
import com.xxr.kb.dtos.KnowledgeBaseRequest;
import com.xxr.service.KnowledgeService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/user/knowledge-base")
@Api(tags = "知识库接口")
public class KnowledgeController {
    @Autowired
    private KnowledgeService knowledgeService;

    /**
     * 获取知识库列表
     * @param knowledgeBaseQueryDTO
     * @return
     */
    @GetMapping("/list")
    @ApiOperation(value = "获取知识库列表")
    public ResponseResult getAllKnowledgeBases( KnowledgeBaseQueryDTO knowledgeBaseQueryDTO){
        return knowledgeService.getList(knowledgeBaseQueryDTO);
    }

    /**
     * 查询知识库详情
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    @ApiOperation(value = "查询知识库详情")
    public ResponseResult selectOne(@PathVariable Long id){
            return knowledgeService.selectKnowledge(id);
    }

    /**
     * 创建个人知识库
     * @param knowledgeBaseRequest
     * @return
     */
    @PostMapping("/create")
    @ApiOperation(value = "创建个人知识库")
    public ResponseResult createKnowledge(@RequestBody KnowledgeBaseRequest knowledgeBaseRequest){
        return knowledgeService.createKnowledge(knowledgeBaseRequest);
    }

    /**
*//** ,修改知识库
     * @param knowledgeBaseRequest
     * @return
     */
    @PostMapping("/update")
    @ApiOperation("修改知识库内容")
    public ResponseResult updateKnowledge(@RequestBody KnowledgeBaseRequest knowledgeBaseRequest){
        return knowledgeService.updateKnowledge(knowledgeBaseRequest);
    }

    /**
     * 收藏/取消收藏知识库
     * @param kbId
     * @return
     */
    @PutMapping("/{kbId}/favorite")
    @ApiOperation(value = "收藏/取消收藏知识库")
    public ResponseResult favorite(@PathVariable Long kbId){
        return knowledgeService.favoriteKnowledge(kbId);
    }

    /**
     * 删除自有知识库
     * @param id
     * @return
     */
    @DeleteMapping("/{kbId}")
    @ApiOperation(value = "删除自有知识库")
    public ResponseResult delete(@PathVariable Long kbId){
        return knowledgeService.deleteKnowledge(kbId);
    }
    /**
     * 查询知识库名称
     * @return
     */
    @GetMapping("/name")
    @ApiOperation(value = "查询知识库名称")
    public ResponseResult selectKnowledgeName(){
        return knowledgeService.selectKnowledgeName();
    }
}
