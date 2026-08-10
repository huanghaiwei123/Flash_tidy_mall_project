package com.gdou.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gdou.common.Result;
import com.gdou.pojo.dto.UserAddressDto;
import com.gdou.pojo.entity.UserAddress;
import com.gdou.service.UserAddressService;
import com.gdou.mapper.UserAddressMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

/**
* @author huanghaiwei
* @description 针对表【user_address(用户收货地址表)】的数据库操作Service实现
* @createDate 2026-08-06 21:59:46
*/
@Service
@Slf4j
public class UserAddressServiceImpl extends ServiceImpl<UserAddressMapper, UserAddress>
    implements UserAddressService{
   @Autowired
   private UserAddressMapper userAddressMapper;
    @Override
    @Transactional
    public Result insert(Long userId, UserAddressDto userAddressDto) {
        // 设为默认时，先清除该用户其他地址的默认标记，保证默认地址唯一
        if (Integer.valueOf(1).equals(userAddressDto.getIsDefault())) {
            clearDefaultExcept(userId, null);
        }
        UserAddress userAddress = new UserAddress();
        BeanUtils.copyProperties(userAddressDto, userAddress);
        userAddress.setUserId(userId);
        userAddress.setCreateTime(new Date());
        userAddress.setUpdateTime(new Date());
        userAddressMapper.insert(userAddress);
        return Result.success("地址添加成功，{}",userAddressDto);
    }

    @Override
    public Result query(Long userId) {
        LambdaQueryWrapper<UserAddress> userAddressLambdaQueryWrapper = new LambdaQueryWrapper<>();
        userAddressLambdaQueryWrapper.eq(UserAddress::getUserId, userId);
        List<UserAddress> userAddresses = userAddressMapper.selectList(userAddressLambdaQueryWrapper);
        if(userAddresses.isEmpty()){
            log.info("当前还没有添加过收货地址");
            return Result.Fail("当前还没有收货地址");
        }
        return Result.success(userAddresses);
    }

    @Override
    @Transactional
    public Result delete(Long userId, Long id) {
        LambdaUpdateWrapper<UserAddress> userAddressLambdaUpdateWrapper = new LambdaUpdateWrapper<>();
        userAddressLambdaUpdateWrapper.eq(UserAddress::getUserId, userId)
                .eq(UserAddress::getId, id);
        int delete = userAddressMapper.delete(userAddressLambdaUpdateWrapper);
        if(delete == 0){
            log.warn("用户{}要删除的地址{}不存在",userId,id);
            return Result.Fail("没有这个地址能删除");
        }
        return Result.success("地址删除成功");
    }

    @Override
    @Transactional
    public Result update(Long userId, UserAddressDto userAddressDto) {
        if (userAddressDto.getId() == null) {
            return Result.Fail("地址ID不能为空");
        }
        UserAddress userAddress = new UserAddress();
        BeanUtils.copyProperties(userAddressDto, userAddress);
        userAddress.setUserId(userId);
        userAddress.setUpdateTime(new Date());
        LambdaUpdateWrapper<UserAddress> userAddressLambdaUpdateWrapper = new LambdaUpdateWrapper<>();
        userAddressLambdaUpdateWrapper.eq(UserAddress::getUserId, userId)
                .eq(UserAddress::getId, userAddressDto.getId());
        int update = userAddressMapper.update(userAddress, userAddressLambdaUpdateWrapper);
        if (update == 0) {
            log.warn("用户{}要修改的地址{}不存在",userId,userAddressDto.getId());
            return Result.Fail("没有这个地址能修改");
        }
        // 修改为默认时，清除该用户其他地址的默认标记，保证默认地址唯一
        if (Integer.valueOf(1).equals(userAddressDto.getIsDefault())) {
            clearDefaultExcept(userId, userAddressDto.getId());
        }
        return Result.success("修改成功");
    }

    /**
     * 将指定用户的非目标地址全部置为非默认；目标地址为 null 表示清除该用户所有默认标记
     * @param userId    用户ID
     * @param exceptId  需要保留为默认的地址ID（可空）
     */
    private void clearDefaultExcept(Long userId, Long exceptId) {
        LambdaUpdateWrapper<UserAddress> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(UserAddress::getUserId, userId)
                .eq(UserAddress::getIsDefault, 1)
                .set(UserAddress::getIsDefault, 0);
        if (exceptId != null) {
            wrapper.ne(UserAddress::getId, exceptId);
        }
        userAddressMapper.update(null, wrapper);
    }
}




