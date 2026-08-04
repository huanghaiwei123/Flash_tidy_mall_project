package com.gdou.Enum;
import com.gdou.Constant.ResultCodeConstant;
public enum SeckillEnum {
    UNPAY(ResultCodeConstant.UNPAY,"订单未支付"),
    ISPAY(ResultCodeConstant.PAY_SUCCESS,"订单已支付"),
    REFUND(ResultCodeConstant.REFUND,"已退款"),
    CANCELLED(ResultCodeConstant.PAY_CANCEL,"取消支付");
    private Integer code;
    private String msg;
    SeckillEnum(Integer code,String msg){
        this.code=code;
        this.msg=msg;
    }
    public static SeckillEnum getSeckillEnum(Integer code){
        for(SeckillEnum seckillEnum:values()){
            if(seckillEnum.code.equals(code)){
                return seckillEnum;
            }
        }
        throw new IllegalArgumentException("未知的错误码"+code);
    }
}
