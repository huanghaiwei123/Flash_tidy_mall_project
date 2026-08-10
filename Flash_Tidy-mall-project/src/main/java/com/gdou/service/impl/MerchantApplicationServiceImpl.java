package com.gdou.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.gdou.common.Result;
import com.gdou.exception.BusinessException;
import com.gdou.mapper.MerchantApplicationMapper;
import com.gdou.mapper.RoleMapper;
import com.gdou.mapper.UserRoleMapper;
import com.gdou.pojo.dto.MerchantApplyDto;
import com.gdou.pojo.dto.MerchantReviewDto;
import com.gdou.pojo.entity.MerchantApplication;
import com.gdou.pojo.entity.Role;
import com.gdou.pojo.entity.UserRole;
import com.gdou.service.MerchantApplicationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

/**
 * 商家申请服务实现
 */
@Service
@Slf4j
public class MerchantApplicationServiceImpl implements MerchantApplicationService {

    /** 申请状态 */
    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_APPROVED = "APPROVED";
    private static final String STATUS_REJECTED = "REJECTED";

    @Autowired
    private MerchantApplicationMapper applicationMapper;

    @Autowired
    private UserRoleMapper userRoleMapper;

    @Autowired
    private RoleMapper roleMapper;

    @Override
    @Transactional
    public Result apply(Long userId, MerchantApplyDto dto) {
        // 1. 检查是否已经是商家
        LambdaQueryWrapper<UserRole> urWrapper = new LambdaQueryWrapper<>();
        urWrapper.eq(UserRole::getUserId, userId);
        List<UserRole> userRoles = userRoleMapper.selectList(urWrapper);
        for (UserRole ur : userRoles) {
            Role role = roleMapper.selectById(ur.getRoleId());
            if (role != null && "MERCHANT".equals(role.getCode())) {
                throw new BusinessException("您已经是商家，无需重复申请");
            }
        }

        // 2. 检查是否已有待审核的申请
        LambdaQueryWrapper<MerchantApplication> appWrapper = new LambdaQueryWrapper<>();
        appWrapper.eq(MerchantApplication::getUserId, userId);
        appWrapper.eq(MerchantApplication::getStatus, STATUS_PENDING);
        Long pendingCount = applicationMapper.selectCount(appWrapper);
        if (pendingCount > 0) {
            throw new BusinessException("您已有待审核的申请，请耐心等待");
        }

        // 3. 创建申请
        MerchantApplication application = new MerchantApplication();
        application.setUserId(userId);
        application.setShopName(dto.getShopName());
        application.setShopDescription(dto.getShopDescription());
        application.setStatus(STATUS_PENDING);
        application.setCreateTime(new Date());
        application.setUpdateTime(new Date());
        applicationMapper.insert(application);

        log.info("用户{}提交商家申请，店铺名: {}", userId, dto.getShopName());
        return Result.success("申请已提交，请等待管理员审核");
    }

    @Override
    @Transactional
    public Result review(Long adminId, MerchantReviewDto dto) {
        // 1. 查找申请
        MerchantApplication application = applicationMapper.selectById(dto.getApplicationId());
        if (application == null) {
            throw new BusinessException("申请不存在");
        }
        if (!STATUS_PENDING.equals(application.getStatus())) {
            throw new BusinessException("该申请已被处理，无需重复审核");
        }

        // 2. 更新申请状态
        application.setStatus(dto.getStatus());
        application.setReviewComment(dto.getComment());
        application.setReviewerId(adminId);
        application.setUpdateTime(new Date());
        applicationMapper.updateById(application);

        // 3. 如果通过，写入 user_role
        if (STATUS_APPROVED.equals(dto.getStatus())) {
            // 查找 MERCHANT 角色
            LambdaQueryWrapper<Role> roleWrapper = new LambdaQueryWrapper<>();
            roleWrapper.eq(Role::getCode, "MERCHANT");
            Role merchantRole = roleMapper.selectOne(roleWrapper);
            if (merchantRole == null) {
                throw new BusinessException("系统错误：MERCHANT 角色不存在");
            }

            // 检查是否已有该角色（防止重复添加）
            LambdaQueryWrapper<UserRole> urWrapper = new LambdaQueryWrapper<>();
            urWrapper.eq(UserRole::getUserId, application.getUserId());
            urWrapper.eq(UserRole::getRoleId, merchantRole.getId());
            Long existed = userRoleMapper.selectCount(urWrapper);
            if (existed == 0) {
                UserRole userRole = new UserRole();
                userRole.setUserId(application.getUserId());
                userRole.setRoleId(merchantRole.getId());
                userRole.setCreateTime(new Date());
                userRoleMapper.insert(userRole);
                log.info("用户{}已成为商家，店铺: {}", application.getUserId(), application.getShopName());
            }
        }

        log.info("管理员{}审核申请#{}，结果: {}", adminId, dto.getApplicationId(), dto.getStatus());
        return Result.success(STATUS_APPROVED.equals(dto.getStatus()) ? "已通过" : "已拒绝");
    }

    @Override
    public Result listApplications(String status) {
        String queryStatus = (status == null || status.isEmpty()) ? STATUS_PENDING : status;
        LambdaQueryWrapper<MerchantApplication> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MerchantApplication::getStatus, queryStatus);
        wrapper.orderByAsc(MerchantApplication::getCreateTime);
        List<MerchantApplication> list = applicationMapper.selectList(wrapper);
        return Result.success(list);
    }
}
