package com.gdou.service;

import com.gdou.common.Result;
import com.gdou.pojo.dto.CategoryDto;
import com.gdou.pojo.entity.Category;
import com.baomidou.mybatisplus.extension.service.IService;

/**
* @author huanghaiwei
* @description 针对表【category(商品分类表（树形）)】的数据库操作Service
* @createDate 2026-08-06 21:59:46
*/
public interface CategoryService extends IService<Category> {

    /**
     * 管理员创建分类
     */
    Result createCategory(CategoryDto categoryDto);

    /**
     * 管理员更新分类
     */
    Result updateCategory(Long categoryId, CategoryDto categoryDto);

    /**
     * 管理员删除分类（叶子节点才能删除）
     */
    Result deleteCategory(Long categoryId);

    /**
     * 管理员查询分类树
     */
    Result adminQueryTree();

    /**
     * 用户端查询分类树（只返回可见的）
     */
    Result userQueryTree();
}
