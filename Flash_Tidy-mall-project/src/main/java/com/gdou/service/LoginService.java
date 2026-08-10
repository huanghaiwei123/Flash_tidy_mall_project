package com.gdou.service;

import com.gdou.common.Result;
import com.gdou.pojo.dto.LoginDto;

public interface LoginService {

    Result login(LoginDto loginDto);
}
