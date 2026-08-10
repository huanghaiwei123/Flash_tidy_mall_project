package com.gdou.controller.admin;

import com.gdou.common.Result;
import com.gdou.pojo.dto.CategoryDto;
import com.gdou.service.CategoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

/**
 * 管理员 — 分类管理
 */
@RestController
@RequestMapping("/hhw/admin/category")
@Slf4j
public class CategoryController {

    @Autowired
    private CategoryService categoryService;

    /**
     * 创建分类
     */
    @PostMapping
    public Result create(@Valid @RequestBody CategoryDto categoryDto) {
        log.info("管理员创建分类");
        return categoryService.createCategory(categoryDto);
    }

    /**
     * 更新分类
     */
    @PutMapping("/{categoryId}")
    public Result update(@PathVariable Long categoryId,
                         @Valid @RequestBody CategoryDto categoryDto) {
        log.info("管理员更新分类：{}", categoryId);
        return categoryService.updateCategory(categoryId, categoryDto);
    }

    /**
     * 删除分类
     */
    @DeleteMapping("/{categoryId}")
    public Result delete(@PathVariable Long categoryId) {
        log.info("管理员删除分类：{}", categoryId);
        return categoryService.deleteCategory(categoryId);
    }

    /**
     * 查询分类树（管理端，含隐藏分类）
     */
    @GetMapping("/tree")
    public Result tree() {
        return categoryService.adminQueryTree();
    }
}
