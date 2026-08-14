package com.gdou.pojo.es;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.util.Date;

/**
 * 商品 SPU 的 Elasticsearch 文档（对应索引 spu）
 * 字段与用户端 SpuListVo 对齐，搜索 + 展示一次到位，避免回查 MySQL
 */
@Document(indexName = "spu")
@Data
public class SpuDoc {

    @Id
    private Long id;

    /** 商品名称 */
    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String name;

    /** 商品描述 */
    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String description;

    /** 品牌 */
    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String brand;

    /** 所属分类 ID（过滤用） */
    @Field(type = FieldType.Long)
    private Long categoryId;

    /** 所属分类名称 */
    @Field(type = FieldType.Keyword)
    private String categoryName;

    /** 商家 ID */
    @Field(type = FieldType.Long)
    private Long merchantId;

    /** 主图 URL（只展示不索引） */
    @Field(type = FieldType.Keyword, index = false)
    private String mainImage;

    /** 销量（排序用） */
    @Field(type = FieldType.Integer)
    private Integer sales;

    /** 状态：0=下架 1=上架（过滤用） */
    @Field(type = FieldType.Integer)
    private Integer status;

    /** 最低上架 SKU 售价（反规范化，展示用） */
    @Field(type = FieldType.Double)
    private Double minPrice;

    /** 上架 SKU 数量（反规范化，展示用） */
    @Field(type = FieldType.Integer)
    private Integer skuCount;

    /** 创建时间（排序用） */
    @Field(type = FieldType.Date)
    private Date createTime;
}
