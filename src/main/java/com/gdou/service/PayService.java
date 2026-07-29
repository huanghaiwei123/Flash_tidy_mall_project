package com.gdou.service;

import com.gdou.common.Result;

public interface PayService {
    Result pay(Long orderId);

    Result cancel(Long orderId);
}
