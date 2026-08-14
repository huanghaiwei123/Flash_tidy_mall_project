package com.gdou.service;

import com.gdou.common.Result;
import com.gdou.pojo.dto.OrderDto;

public interface UserSeckillService {
    Result query();

    Result getCaptcha(Long userId);

    Result start(Long userId, OrderDto orderDto,String orderType);
}
