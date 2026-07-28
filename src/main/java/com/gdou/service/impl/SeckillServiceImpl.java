package com.gdou.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gdou.common.Result;
import com.gdou.pojo.entity.Seckill;
import com.gdou.service.SeckillService;
import com.gdou.mapper.SeckillMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/**
* @author huanghaiwei
* @description 针对表【seckill(秒杀库存表)】的数据库操作Service实现
* @createDate 2026-07-26 16:48:10
*/
@Service
public class SeckillServiceImpl extends ServiceImpl<SeckillMapper, Seckill>
    implements SeckillService{
    @Autowired
    private SeckillMapper seckillMapper;

    /**
     * 获取秒杀商品列表
     * @return
     */
    @Override
    public List<Seckill> getSeckillList() {
        LambdaQueryWrapper<Seckill> wrapper = new LambdaQueryWrapper<>();
        return  seckillMapper.selectList(wrapper);
    }

    /**
     * 秒杀业务具体实现
     * @return
     */
    @Override
    public Result onSeckill() {
        return null;
    }
}




