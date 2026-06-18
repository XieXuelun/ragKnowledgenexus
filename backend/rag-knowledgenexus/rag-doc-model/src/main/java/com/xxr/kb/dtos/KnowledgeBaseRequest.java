package com.xxr.kb.dtos;

import lombok.Data;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.util.List;

@Data
public class KnowledgeBaseRequest {
    private Long id;
    @NotBlank(message = "知识库名称不能为空")
    @Size(max = 100, message = "知识库名称最大100个字符")
    private String name;

    @Size(max = 500, message = "描述最大500个字符")
    private String description;

    private String icon;

    private Integer visibility;  // 可见范围: 0=私有, 1=公开, 2=部门可见

    private List<Long> accessibleUserIds;  // 可访问用户ID列表

    private Long deptId;  // 所属部门ID

    private List<Long> accessibleDeptIds;  // 可访问部门ID列表
}