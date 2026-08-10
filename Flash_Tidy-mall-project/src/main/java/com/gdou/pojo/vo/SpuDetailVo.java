package com.gdou.pojo.vo;

import com.gdou.pojo.entity.Sku;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * 用户端 SPU 详情 VO
 */
@Data
public class SpuDetailVo {

    private Long id;
    private String name;
    private String description;
    private String categoryName;
    private String brand;
    private String mainImage;
    private Object images;
    private String detail;
    private Integer sales;
    private Date createTime;

    /** 该 SPU 下的所有上架 SKU */
    private List<Sku> skuList;
}
