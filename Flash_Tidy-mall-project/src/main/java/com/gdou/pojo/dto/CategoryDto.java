package com.gdou.pojo.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 分类创建/更新 DTO
 */
@Data
public class CategoryDto {

    /**
     * 父分类 ID，0 或 null 表示一级分类
     */
    private Long parentId;

    /**
     * 分类名称
     */
    @NotBlank(message = "分类名称不能为空")
    @Size(max = 64, message = "分类名称最长 64 字")
    private String name;

    /**
     * 图标 URL
     */
    private String icon;

    /**
     * 排序值（越小越靠前）
     */
    private Integer sort;

    /**
     * 状态：0=隐藏 1=显示
     */
    private Integer status;
}
