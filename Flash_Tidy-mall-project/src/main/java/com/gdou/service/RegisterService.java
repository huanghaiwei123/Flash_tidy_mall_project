package com.gdou.service;

import com.gdou.common.Result;
import com.gdou.pojo.dto.RegisterDto;

public interface RegisterService {
    Result register(RegisterDto registerDto);
}
