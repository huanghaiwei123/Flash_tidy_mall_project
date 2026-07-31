package com.gdou.service;

import com.gdou.common.Result;
import com.gdou.pojo.entity.UserSeckillRecord;
import com.baomidou.mybatisplus.extension.service.IService;

/**
* @author huanghaiwei
* @description 针对表【user_seckill_record(用户秒杀记录表)】的数据库操作Service
* @createDate 2026-07-26 16:48:10
*/
public interface UserSeckillRecordService extends IService<UserSeckillRecord> {
    Result seckillResult();
}
