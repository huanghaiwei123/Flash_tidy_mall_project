package com.gdou.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gdou.pojo.entity.OrderItem;
import com.gdou.service.OrderItemService;
import com.gdou.mapper.OrderItemMapper;
import org.springframework.stereotype.Service;

/**
* @author huanghaiwei
* @description 针对表【order_item(订单明细表)】的数据库操作Service实现
* @createDate 2026-08-06 21:59:46
*/
@Service
public class OrderItemServiceImpl extends ServiceImpl<OrderItemMapper, OrderItem>
    implements OrderItemService{

}




