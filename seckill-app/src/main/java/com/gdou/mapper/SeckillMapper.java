package com.gdou.mapper;

import com.gdou.pojo.entity.Seckill;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Update;

/**
* @author huanghaiwei
* @description 针对表【seckill(秒杀库存表)】的数据库操作Mapper
* @createDate 2026-07-26 16:48:10
* @Entity com.gdou.entity.Seckill
*/
public interface SeckillMapper extends BaseMapper<Seckill> {
    @Update("update seckill set number=number-1 where number>0 and seckill_id=#{seckillId}")
    int deductById(Long seckillId);

    @Update("update seckill set number = number + 1 where seckill_id = #{seckillId}")
    int incrementById(Long seckillId);

}




