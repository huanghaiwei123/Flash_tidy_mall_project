package com.gdou.pojo.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 用户端 SPU 列表项 VO
 */
@Data
public class SpuListVo {

    private Long id;
    private String name;
    private String description;
    private String categoryName;
    private String brand;
    private String mainImage;
    private Integer sales;
    private Integer status;
    /** 该 SPU 下最低 SKU 售价 */
    private BigDecimal minPrice;
    /** 该 SPU 下 SKU 数量 */
    private Integer skuCount;
}
