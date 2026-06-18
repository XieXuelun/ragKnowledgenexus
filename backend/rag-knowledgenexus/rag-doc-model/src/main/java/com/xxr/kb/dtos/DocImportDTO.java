package com.xxr.kb.dtos;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.constraints.NotNull;

@Data
@ApiModel(description = "导入文档到知识库请求参数")
public class DocImportDTO {

    @ApiModelProperty(value = "知识库ID", required = true, hidden = true) // 路径参数，DTO中仅做传递，Swagger隐藏
    @NotNull(message = "知识库ID不能为空")
    private Long kbId;

    @ApiModelProperty(value = "上传的文档文件", required = true)
    @NotNull(message = "请选择要上传的文档文件")
    private MultipartFile file;

    @ApiModelProperty(value = "文档自定义标题（可选，不填则使用文件名）")
    private String title;

    @ApiModelProperty(value = "是否自动解析文档内容（默认true）")
    private Boolean autoParse = true;
}