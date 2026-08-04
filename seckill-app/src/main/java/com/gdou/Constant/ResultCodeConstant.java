package com.gdou.Constant;

public class ResultCodeConstant {
    public static final Integer SUCCESS = 200;
    public static final Integer FAIL = 500;
    public static final Integer ERROR = 400;
    /**
     * 登录字段
     */
    public static final Integer LOGIN_ERROR = 401;
    public static final Integer LOGOUT_ERROR = 402;
    public static final Integer NOT_LOGIN = 403;
    public static final Integer LOGIN_FAIL = 404;
    public static final Integer LOGOUT_FAIL = 405;
    /**
     * 秒杀业务字段
     */
    public static final Integer SeckillClose = 601;
    public static final Integer RepeatKill = 602;
    public static final Integer SeckillOver = 603;
    public static final Integer StockEmpty = 604;
    public static final Integer SeckillFail=605;
    /**
     * 支付字段
     */
    public static final Integer UNPAY = 1000;
    public static final Integer PAY_CANCEL = 1001;
    public static final Integer PAY_SUCCESS = 1002;
    public static final Integer REFUND = 1003;

}
