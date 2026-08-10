package com.gdou.controller.merchant;

import com.gdou.common.Result;
import com.gdou.pojo.dto.SpuDto;
import com.gdou.service.SpuService;
import com.gdou.util.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/hhw/merchant")
@Slf4j
public class MerchantSpuController {
    @Autowired
    private SpuService spuService;

    /**
     * 商家查看自家店铺商品
     */
    @GetMapping("/spu/query")
    public Result spuQuery(@RequestParam Long merchantId){
        log.info("商家{}正在查看自己店铺商品", merchantId);
        return spuService.spuQuery(merchantId);
    }

    /**
     * 商家为自家店铺添加商品
     */
    @PostMapping("/spu")
    public Result spuSave(@Valid @RequestBody SpuDto spuDto){
        Long merchantId = UserHolder.get();
        log.info("商家{}正在为自家店铺上架商品", merchantId);
        return spuService.spuSave(merchantId, spuDto);
    }

    /**
     * 商家更新商品
     */
    @PutMapping("/spu/{spuId}")
    public Result spuUpdate(@PathVariable Long spuId,
                            @Valid @RequestBody SpuDto spuDto) {
        Long merchantId = UserHolder.get();
        log.info("商家{}正在更新商品{}", merchantId, spuId);
        return spuService.spuUpdate(merchantId, spuId, spuDto);
    }

    /**
     * 商家上下架商品
     */
    @PutMapping("/spu/{spuId}/status")
    public Result spuUpdateStatus(@PathVariable Long spuId,
                                  @RequestParam Integer status) {
        Long merchantId = UserHolder.get();
        log.info("商家{}修改商品{}状态为{}", merchantId, spuId, status);
        return spuService.spuUpdateStatus(merchantId, spuId, status);
    }
}
