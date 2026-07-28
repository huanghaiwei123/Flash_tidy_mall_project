package com.gdou.service;

import com.gdou.common.Result;
import com.gdou.pojo.entity.SeckillUser;
import org.springframework.stereotype.Service;


public interface RegisterService {
    Result register(SeckillUser seckillUser);
}
