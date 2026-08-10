package com.gdou.pojo.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 分类树节点 VO
 */
@Data
public class CategoryTreeVo {

    private Long id;
    private Long parentId;
    private String name;
    private String icon;
    private Integer level;
    private Integer sort;
    private Integer isLeaf;
    private Integer status;
    private List<CategoryTreeVo> children = new ArrayList<>();
}
