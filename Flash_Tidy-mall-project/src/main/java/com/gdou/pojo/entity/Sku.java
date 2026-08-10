package com.gdou.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * SKU 库存单元表（含秒杀 + 库存三字段）
 * @TableName sku
 */
@TableName(value ="sku")
public class Sku implements Serializable {
    /**
     * SKU ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 所属 SPU ID
     */
    private Long spuId;

    /**
     * SKU 名称（规格组合，如 "iPhone 15 黑色 256G"）
     */
    private String name;

    /**
     * 规格描述（JSON，如 [{"k":"颜色","v":"黑色"},{"k":"容量","v":"256G"}]）
     */
    private String spec;

    /**
     * 售价
     */
    private BigDecimal price;

    /**
     * 原价/划线价
     */
    private BigDecimal originalPrice;

    /**
     * SKU 图片 URL
     */
    private String image;

    /**
     * 总库存（管理员设置）
     */
    private Integer stock;

    /**
     * 锁定库存（下单未付）
     */
    private Integer lockedStock;

    /**
     * 可售库存（= stock - locked_stock）
     */
    private Integer availableStock;

    /**
     * 促销/秒杀库存配额（≤ stock）
     */
    private Integer promotionStock;

    /**
     * 是否参与秒杀：0=否 1=是
     */
    private Integer isSeckill;

    /**
     * 秒杀价格
     */
    private BigDecimal seckillPrice;

    /**
     * 秒杀开始时间
     */
    private Date seckillStartTime;

    /**
     * 秒杀结束时间
     */
    private Date seckillEndTime;

    /**
     * 排序值（越小越靠前）
     */
    private Integer sort;

    /**
     * 状态：0=下架 1=上架
     */
    private Integer status;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

    /**
     * SKU ID
     */
    public Long getId() {
        return id;
    }

    /**
     * SKU ID
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * 所属 SPU ID
     */
    public Long getSpuId() {
        return spuId;
    }

    /**
     * 所属 SPU ID
     */
    public void setSpuId(Long spuId) {
        this.spuId = spuId;
    }

    /**
     * SKU 名称（规格组合，如 "iPhone 15 黑色 256G"）
     */
    public String getName() {
        return name;
    }

    /**
     * SKU 名称（规格组合，如 "iPhone 15 黑色 256G"）
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * 规格描述（JSON，如 [{"k":"颜色","v":"黑色"},{"k":"容量","v":"256G"}]）
     */
    public String getSpec() {
        return spec;
    }

    /**
     * 规格描述（JSON，如 [{"k":"颜色","v":"黑色"},{"k":"容量","v":"256G"}]）
     */
    public void setSpec(String spec) {
        this.spec = spec;
    }

    /**
     * 售价
     */
    public BigDecimal getPrice() {
        return price;
    }

    /**
     * 售价
     */
    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    /**
     * 原价/划线价
     */
    public BigDecimal getOriginalPrice() {
        return originalPrice;
    }

    /**
     * 原价/划线价
     */
    public void setOriginalPrice(BigDecimal originalPrice) {
        this.originalPrice = originalPrice;
    }

    /**
     * SKU 图片 URL
     */
    public String getImage() {
        return image;
    }

    /**
     * SKU 图片 URL
     */
    public void setImage(String image) {
        this.image = image;
    }

    /**
     * 总库存（管理员设置）
     */
    public Integer getStock() {
        return stock;
    }

    /**
     * 总库存（管理员设置）
     */
    public void setStock(Integer stock) {
        this.stock = stock;
    }

    /**
     * 锁定库存（下单未付）
     */
    public Integer getLockedStock() {
        return lockedStock;
    }

    /**
     * 锁定库存（下单未付）
     */
    public void setLockedStock(Integer lockedStock) {
        this.lockedStock = lockedStock;
    }

    /**
     * 可售库存（= stock - locked_stock）
     */
    public Integer getAvailableStock() {
        return availableStock;
    }

    /**
     * 可售库存（= stock - locked_stock）
     */
    public void setAvailableStock(Integer availableStock) {
        this.availableStock = availableStock;
    }

    /**
     * 促销/秒杀库存配额（≤ stock）
     */
    public Integer getPromotionStock() {
        return promotionStock;
    }

    /**
     * 促销/秒杀库存配额（≤ stock）
     */
    public void setPromotionStock(Integer promotionStock) {
        this.promotionStock = promotionStock;
    }

    /**
     * 是否参与秒杀：0=否 1=是
     */
    public Integer getIsSeckill() {
        return isSeckill;
    }

    /**
     * 是否参与秒杀：0=否 1=是
     */
    public void setIsSeckill(Integer isSeckill) {
        this.isSeckill = isSeckill;
    }

    /**
     * 秒杀价格
     */
    public BigDecimal getSeckillPrice() {
        return seckillPrice;
    }

    /**
     * 秒杀价格
     */
    public void setSeckillPrice(BigDecimal seckillPrice) {
        this.seckillPrice = seckillPrice;
    }

    /**
     * 秒杀开始时间
     */
    public Date getSeckillStartTime() {
        return seckillStartTime;
    }

    /**
     * 秒杀开始时间
     */
    public void setSeckillStartTime(Date seckillStartTime) {
        this.seckillStartTime = seckillStartTime;
    }

    /**
     * 秒杀结束时间
     */
    public Date getSeckillEndTime() {
        return seckillEndTime;
    }

    /**
     * 秒杀结束时间
     */
    public void setSeckillEndTime(Date seckillEndTime) {
        this.seckillEndTime = seckillEndTime;
    }

    /**
     * 排序值（越小越靠前）
     */
    public Integer getSort() {
        return sort;
    }

    /**
     * 排序值（越小越靠前）
     */
    public void setSort(Integer sort) {
        this.sort = sort;
    }

    /**
     * 状态：0=下架 1=上架
     */
    public Integer getStatus() {
        return status;
    }

    /**
     * 状态：0=下架 1=上架
     */
    public void setStatus(Integer status) {
        this.status = status;
    }

    /**
     * 创建时间
     */
    public Date getCreateTime() {
        return createTime;
    }

    /**
     * 创建时间
     */
    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    /**
     * 更新时间
     */
    public Date getUpdateTime() {
        return updateTime;
    }

    /**
     * 更新时间
     */
    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }

    @Override
    public boolean equals(Object that) {
        if (this == that) {
            return true;
        }
        if (that == null) {
            return false;
        }
        if (getClass() != that.getClass()) {
            return false;
        }
        Sku other = (Sku) that;
        return (this.getId() == null ? other.getId() == null : this.getId().equals(other.getId()))
            && (this.getSpuId() == null ? other.getSpuId() == null : this.getSpuId().equals(other.getSpuId()))
            && (this.getName() == null ? other.getName() == null : this.getName().equals(other.getName()))
            && (this.getSpec() == null ? other.getSpec() == null : this.getSpec().equals(other.getSpec()))
            && (this.getPrice() == null ? other.getPrice() == null : this.getPrice().equals(other.getPrice()))
            && (this.getOriginalPrice() == null ? other.getOriginalPrice() == null : this.getOriginalPrice().equals(other.getOriginalPrice()))
            && (this.getImage() == null ? other.getImage() == null : this.getImage().equals(other.getImage()))
            && (this.getStock() == null ? other.getStock() == null : this.getStock().equals(other.getStock()))
            && (this.getLockedStock() == null ? other.getLockedStock() == null : this.getLockedStock().equals(other.getLockedStock()))
            && (this.getAvailableStock() == null ? other.getAvailableStock() == null : this.getAvailableStock().equals(other.getAvailableStock()))
            && (this.getPromotionStock() == null ? other.getPromotionStock() == null : this.getPromotionStock().equals(other.getPromotionStock()))
            && (this.getIsSeckill() == null ? other.getIsSeckill() == null : this.getIsSeckill().equals(other.getIsSeckill()))
            && (this.getSeckillPrice() == null ? other.getSeckillPrice() == null : this.getSeckillPrice().equals(other.getSeckillPrice()))
            && (this.getSeckillStartTime() == null ? other.getSeckillStartTime() == null : this.getSeckillStartTime().equals(other.getSeckillStartTime()))
            && (this.getSeckillEndTime() == null ? other.getSeckillEndTime() == null : this.getSeckillEndTime().equals(other.getSeckillEndTime()))
            && (this.getSort() == null ? other.getSort() == null : this.getSort().equals(other.getSort()))
            && (this.getStatus() == null ? other.getStatus() == null : this.getStatus().equals(other.getStatus()))
            && (this.getCreateTime() == null ? other.getCreateTime() == null : this.getCreateTime().equals(other.getCreateTime()))
            && (this.getUpdateTime() == null ? other.getUpdateTime() == null : this.getUpdateTime().equals(other.getUpdateTime()));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getId() == null) ? 0 : getId().hashCode());
        result = prime * result + ((getSpuId() == null) ? 0 : getSpuId().hashCode());
        result = prime * result + ((getName() == null) ? 0 : getName().hashCode());
        result = prime * result + ((getSpec() == null) ? 0 : getSpec().hashCode());
        result = prime * result + ((getPrice() == null) ? 0 : getPrice().hashCode());
        result = prime * result + ((getOriginalPrice() == null) ? 0 : getOriginalPrice().hashCode());
        result = prime * result + ((getImage() == null) ? 0 : getImage().hashCode());
        result = prime * result + ((getStock() == null) ? 0 : getStock().hashCode());
        result = prime * result + ((getLockedStock() == null) ? 0 : getLockedStock().hashCode());
        result = prime * result + ((getAvailableStock() == null) ? 0 : getAvailableStock().hashCode());
        result = prime * result + ((getPromotionStock() == null) ? 0 : getPromotionStock().hashCode());
        result = prime * result + ((getIsSeckill() == null) ? 0 : getIsSeckill().hashCode());
        result = prime * result + ((getSeckillPrice() == null) ? 0 : getSeckillPrice().hashCode());
        result = prime * result + ((getSeckillStartTime() == null) ? 0 : getSeckillStartTime().hashCode());
        result = prime * result + ((getSeckillEndTime() == null) ? 0 : getSeckillEndTime().hashCode());
        result = prime * result + ((getSort() == null) ? 0 : getSort().hashCode());
        result = prime * result + ((getStatus() == null) ? 0 : getStatus().hashCode());
        result = prime * result + ((getCreateTime() == null) ? 0 : getCreateTime().hashCode());
        result = prime * result + ((getUpdateTime() == null) ? 0 : getUpdateTime().hashCode());
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append(" [");
        sb.append("Hash = ").append(hashCode());
        sb.append(", id=").append(id);
        sb.append(", spuId=").append(spuId);
        sb.append(", name=").append(name);
        sb.append(", spec=").append(spec);
        sb.append(", price=").append(price);
        sb.append(", originalPrice=").append(originalPrice);
        sb.append(", image=").append(image);
        sb.append(", stock=").append(stock);
        sb.append(", lockedStock=").append(lockedStock);
        sb.append(", availableStock=").append(availableStock);
        sb.append(", promotionStock=").append(promotionStock);
        sb.append(", isSeckill=").append(isSeckill);
        sb.append(", seckillPrice=").append(seckillPrice);
        sb.append(", seckillStartTime=").append(seckillStartTime);
        sb.append(", seckillEndTime=").append(seckillEndTime);
        sb.append(", sort=").append(sort);
        sb.append(", status=").append(status);
        sb.append(", createTime=").append(createTime);
        sb.append(", updateTime=").append(updateTime);
        sb.append(", serialVersionUID=").append(serialVersionUID);
        sb.append("]");
        return sb.toString();
    }
}