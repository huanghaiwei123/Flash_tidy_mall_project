package com.gdou.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gdou.common.Result;
import com.gdou.exception.BusinessException;
import com.gdou.mapper.CategoryMapper;
import com.gdou.mapper.SpuMapper;
import com.gdou.pojo.dto.CategoryDto;
import com.gdou.pojo.entity.Category;
import com.gdou.pojo.entity.Spu;
import com.gdou.pojo.vo.CategoryTreeVo;
import com.gdou.service.CategoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
* @author huanghaiwei
* @description 针对表【category(商品分类表（树形）)】的数据库操作Service实现
* @createDate 2026-08-06 21:59:46
*/
@Service
@Slf4j
public class CategoryServiceImpl extends ServiceImpl<CategoryMapper, Category>
    implements CategoryService{

    @Autowired
    private CategoryMapper categoryMapper;
    @Autowired
    private SpuMapper spuMapper;

    @Override
    @Transactional
    public Result createCategory(CategoryDto categoryDto) {
        Category category = new Category();
        BeanUtils.copyProperties(categoryDto, category);

        // 自动计算层级
        Long parentId = categoryDto.getParentId() != null ? categoryDto.getParentId() : 0L;
        int level = 1;
        if (parentId != 0) {
            Category parent = categoryMapper.selectById(parentId);
            if (parent == null) {
                return Result.Fail("父分类不存在");
            }
            level = parent.getLevel() + 1;
            if (level > 3) {
                return Result.Fail("最多支持三级分类");
            }
            // 父分类不再是叶子节点
            parent.setIsLeaf(0);
            parent.setUpdateTime(new Date());
            categoryMapper.updateById(parent);
        }
        category.setParentId(parentId);
        category.setLevel(level);
        category.setIsLeaf(1); // 新建分类默认是叶子
        if (category.getSort() == null) category.setSort(0);
        if (category.getStatus() == null) category.setStatus(1);
        category.setCreateTime(new Date());
        category.setUpdateTime(new Date());

        save(category);
        log.info("管理员创建分类：{}（层级={}）", category.getName(), level);
        return Result.success("分类创建成功", category);
    }

    @Override
    @Transactional
    public Result updateCategory(Long categoryId, CategoryDto categoryDto) {
        Category category = categoryMapper.selectById(categoryId);
        if (category == null) {
            return Result.Fail("分类不存在");
        }

        Long oldParentId = category.getParentId();
        Long newParentId = categoryDto.getParentId() != null ? categoryDto.getParentId() : oldParentId;

        BeanUtils.copyProperties(categoryDto, category);
        category.setId(categoryId);

        // 如果父分类变了，重新计算层级
        if (!newParentId.equals(oldParentId)) {
            if (newParentId != 0) {
                Category newParent = categoryMapper.selectById(newParentId);
                if (newParent == null) {
                    return Result.Fail("新父分类不存在");
                }
                if (newParent.getLevel() + 1 > 3) {
                    return Result.Fail("最多支持三级分类");
                }
                category.setLevel(newParent.getLevel() + 1);
                // 新父不再是叶子
                newParent.setIsLeaf(0);
                newParent.setUpdateTime(new Date());
                categoryMapper.updateById(newParent);
            } else {
                category.setLevel(1);
            }
            // 旧父检查是否变成叶子
            if (oldParentId != 0) {
                checkAndUpdateLeaf(oldParentId);
            }
        }

        // 如果分类名称变了，同步更新所有关联 SPU 的 categoryName
        String oldName = categoryMapper.selectById(categoryId).getName();
        if (!oldName.equals(category.getName())) {
            LambdaUpdateWrapper<Spu> spuWrapper = new LambdaUpdateWrapper<>();
            spuWrapper.eq(Spu::getCategoryId, categoryId)
                     .set(Spu::getCategoryName, category.getName());
            spuMapper.update(null, spuWrapper);
            log.info("分类 {} 改名，同步更新 {} 个 SPU 的 categoryName", categoryId,
                    spuMapper.selectCount(new LambdaQueryWrapper<Spu>().eq(Spu::getCategoryId, categoryId)));
        }

        category.setUpdateTime(new Date());
        updateById(category);

        log.info("管理员更新分类：{}", category.getName());
        return Result.success("分类更新成功", category);
    }

    @Override
    @Transactional
    public Result deleteCategory(Long categoryId) {
        Category category = categoryMapper.selectById(categoryId);
        if (category == null) {
            return Result.Fail("分类不存在");
        }
        // 检查是否有子分类
        LambdaQueryWrapper<Category> childWrapper = new LambdaQueryWrapper<>();
        childWrapper.eq(Category::getParentId, categoryId);
        Long childCount = categoryMapper.selectCount(childWrapper);
        if (childCount > 0) {
            return Result.Fail("该分类下有子分类，请先删除子分类");
        }

        // 检查是否有关联商品
        LambdaQueryWrapper<Spu> spuWrapper = new LambdaQueryWrapper<>();
        spuWrapper.eq(Spu::getCategoryId, categoryId);
        Long spuCount = spuMapper.selectCount(spuWrapper);
        if (spuCount > 0) {
            return Result.Fail("该分类下有 " + spuCount + " 个商品，请先下架或转移商品后再删除");
        }

        categoryMapper.deleteById(categoryId);

        // 检查父分类是否变成叶子节点
        if (category.getParentId() != 0) {
            checkAndUpdateLeaf(category.getParentId());
        }

        log.info("管理员删除分类：{}", category.getName());
        return Result.success("分类删除成功");
    }

    @Override
    public Result adminQueryTree() {
        List<Category> all = categoryMapper.selectList(null);
        List<CategoryTreeVo> tree = buildTree(all);
        return Result.success(tree);
    }

    @Override
    public Result userQueryTree() {
        LambdaQueryWrapper<Category> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Category::getStatus, 1)
               .orderByAsc(Category::getSort);
        List<Category> visible = categoryMapper.selectList(wrapper);
        List<CategoryTreeVo> tree = buildTree(visible);
        return Result.success(tree);
    }

    // ===================== 内部方法 =====================

    /**
     * 检查并更新父分类的 isLeaf 状态
     */
    private void checkAndUpdateLeaf(Long parentId) {
        LambdaQueryWrapper<Category> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Category::getParentId, parentId);
        Long childrenCount = categoryMapper.selectCount(wrapper);
        Category parent = categoryMapper.selectById(parentId);
        if (parent != null) {
            parent.setIsLeaf(childrenCount == 0 ? 1 : 0);
            parent.setUpdateTime(new Date());
            categoryMapper.updateById(parent);
        }
    }

    /**
     * 构建分类树
     */
    private List<CategoryTreeVo> buildTree(List<Category> categories) {
        Map<Long, CategoryTreeVo> nodeMap = new HashMap<>();
        List<CategoryTreeVo> roots = new ArrayList<>();

        // 全部转成 VO
        for (Category c : categories) {
            CategoryTreeVo vo = new CategoryTreeVo();
            BeanUtils.copyProperties(c, vo);
            nodeMap.put(c.getId(), vo);
        }

        // 构建树
        for (Category c : categories) {
            CategoryTreeVo vo = nodeMap.get(c.getId());
            Long parentId = c.getParentId() != null ? c.getParentId() : 0L;
            if (parentId == 0 || !nodeMap.containsKey(parentId)) {
                roots.add(vo);
            } else {
                nodeMap.get(parentId).getChildren().add(vo);
            }
        }

        // 每层按 sort 排序
        sortTree(roots);
        return roots;
    }

    private void sortTree(List<CategoryTreeVo> nodes) {
        nodes.sort(Comparator.comparingInt(v -> v.getSort() != null ? v.getSort() : 0));
        for (CategoryTreeVo node : nodes) {
            if (!node.getChildren().isEmpty()) {
                sortTree(node.getChildren());
            }
        }
    }
}
