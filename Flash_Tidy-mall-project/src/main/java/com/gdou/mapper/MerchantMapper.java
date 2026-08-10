package com.gdou.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gdou.pojo.entity.Merchant;
import org.apache.ibatis.annotations.Mapper;

/**
 * 商家表 Mapper
 */
@Mapper
public interface MerchantMapper extends BaseMapper<Merchant> {
}
