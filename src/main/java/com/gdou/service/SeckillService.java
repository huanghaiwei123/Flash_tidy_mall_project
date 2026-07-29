package com.gdou.service;

import com.gdou.common.Result;
import com.gdou.pojo.dto.SeckillDto;
import com.gdou.pojo.entity.Seckill;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
* @author huanghaiwei
* @description 针对表【seckill(秒杀库存表)】的数据库操作Service
* @createDate 2026-07-26 16:48:10
*/
@Service
public interface SeckillService extends IService<Seckill> {
    /**
     * 获取商品列表
     */
    List<Seckill> getSeckillList();

    Result onSeckill(Long seckillId, String userId);

    /**
     * 新增秒杀活动
     */
    Result addSeckill(SeckillDto seckillDto);

    /**
     * 修改秒杀活动
     */
    Result updateSeckill(Long id, SeckillDto seckillDto);

    /**
     * 删除秒杀活动
     */
    Result deleteSeckill(Long id);
}
