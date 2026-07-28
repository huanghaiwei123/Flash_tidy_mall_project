package com.gdou.controller;
import com.gdou.common.Result;
import com.gdou.pojo.entity.Seckill;
import com.gdou.service.SeckillService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@Slf4j
@RequestMapping("/hhw/seckill")
public class SeckillController {
    @Autowired
    private SeckillService seckillService;
    /**
     * 获取秒杀商品列表
     * @return
     */
    @GetMapping("/seckills")
    public Result getSeckillList() {
        List<Seckill> list = seckillService.getSeckillList();
        return Result.success(list);
    }

    /**
     * 获取指定商品
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    public Result getSeckillById(@PathVariable("id") Integer id) {
        return Result.success(seckillService.getById(id));
    }

    /**
     * 开启秒杀
     * @param seckillId
     * @return
     */
    @PostMapping()
    public Result onSeckill(@PathVariable Long seckillId) {
        return seckillService.onSeckill();
    }
}
