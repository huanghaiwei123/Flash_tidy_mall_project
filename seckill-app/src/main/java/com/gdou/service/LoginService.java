package com.gdou.service;

import com.gdou.common.Result;
import com.gdou.pojo.entity.SeckillUser;

public interface LoginService {
    Result login(SeckillUser seckillUser, String ip);
}
