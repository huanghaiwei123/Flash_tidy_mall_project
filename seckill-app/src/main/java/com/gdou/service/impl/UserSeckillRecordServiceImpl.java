package com.gdou.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gdou.Constant.ResultCodeConstant;
import com.gdou.common.Result;
import com.gdou.pojo.entity.UserSeckillRecord;
import com.gdou.service.UserSeckillRecordService;
import com.gdou.mapper.UserSeckillRecordMapper;
import com.gdou.util.UserHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
* @author huanghaiwei
* @description 针对表【user_seckill_record(用户秒杀记录表)】的数据库操作Service实现
* @createDate 2026-07-26 16:48:10
*/
@Service
public class UserSeckillRecordServiceImpl extends ServiceImpl<UserSeckillRecordMapper, UserSeckillRecord>
    implements UserSeckillRecordService{
    @Autowired
    private UserSeckillRecordMapper userSeckillRecordMapper;
    @Override
    public Result seckillResult() {
        LambdaQueryWrapper<UserSeckillRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserSeckillRecord::getUserId, UserHolder.getUserId());
        List<UserSeckillRecord> records = userSeckillRecordMapper.selectList(wrapper);
        if (records == null||records.size()==0) {
            return Result.error("小同志，当前还没有秒杀订单哦", ResultCodeConstant.ERROR,null);
        }
        return Result.success(records);
    }
}




