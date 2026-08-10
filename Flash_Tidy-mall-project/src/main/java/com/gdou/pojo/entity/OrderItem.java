package com.gdou.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 订单明细表
 * @TableName order_item
 */
@TableName(value ="order_item")
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderItem implements Serializable {
    /**
     * 明细ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 订单ID
     */
    private Long orderId;

    /**
     * 订单编号（冗余，方便查询）
     */
    private String orderNo;

    /**
     * SPU ID
     */
    private Long spuId;

    /**
     * SKU ID
     */
    private Long skuId;

    /**
     * SKU 名称快照
     */
    private String skuName;

    /**
     * SKU 规格快照
     */
    private String skuSpec;

    /**
     * SKU 图片快照
     */
    private String skuImage;

    /**
     * SKU 下单时单价快照
     */
    private BigDecimal skuPrice;

    /**
     * 购买数量
     */
    private Integer quantity;

    /**
     * 明细总价（= sku_price × quantity）
     */
    private BigDecimal totalPrice;

    /**
     * 创建时间
     */
    private Date createTime;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

    /**
     * 明细ID
     */
    public Long getId() {
        return id;
    }

    /**
     * 明细ID
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * 订单ID
     */
    public Long getOrderId() {
        return orderId;
    }

    /**
     * 订单ID
     */
    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    /**
     * 订单编号（冗余，方便查询）
     */
    public String getOrderNo() {
        return orderNo;
    }

    /**
     * 订单编号（冗余，方便查询）
     */
    public void setOrderNo(String orderNo) {
        this.orderNo = orderNo;
    }

    /**
     * SPU ID
     */
    public Long getSpuId() {
        return spuId;
    }

    /**
     * SPU ID
     */
    public void setSpuId(Long spuId) {
        this.spuId = spuId;
    }

    /**
     * SKU ID
     */
    public Long getSkuId() {
        return skuId;
    }

    /**
     * SKU ID
     */
    public void setSkuId(Long skuId) {
        this.skuId = skuId;
    }

    /**
     * SKU 名称快照
     */
    public String getSkuName() {
        return skuName;
    }

    /**
     * SKU 名称快照
     */
    public void setSkuName(String skuName) {
        this.skuName = skuName;
    }

    /**
     * SKU 规格快照
     */
    public String getSkuSpec() {
        return skuSpec;
    }

    /**
     * SKU 规格快照
     */
    public void setSkuSpec(String skuSpec) {
        this.skuSpec = skuSpec;
    }

    /**
     * SKU 图片快照
     */
    public String getSkuImage() {
        return skuImage;
    }

    /**
     * SKU 图片快照
     */
    public void setSkuImage(String skuImage) {
        this.skuImage = skuImage;
    }

    /**
     * SKU 下单时单价快照
     */
    public BigDecimal getSkuPrice() {
        return skuPrice;
    }

    /**
     * SKU 下单时单价快照
     */
    public void setSkuPrice(BigDecimal skuPrice) {
        this.skuPrice = skuPrice;
    }

    /**
     * 购买数量
     */
    public Integer getQuantity() {
        return quantity;
    }

    /**
     * 购买数量
     */
    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    /**
     * 明细总价（= sku_price × quantity）
     */
    public BigDecimal getTotalPrice() {
        return totalPrice;
    }

    /**
     * 明细总价（= sku_price × quantity）
     */
    public void setTotalPrice(BigDecimal totalPrice) {
        this.totalPrice = totalPrice;
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
        OrderItem other = (OrderItem) that;
        return (this.getId() == null ? other.getId() == null : this.getId().equals(other.getId()))
            && (this.getOrderId() == null ? other.getOrderId() == null : this.getOrderId().equals(other.getOrderId()))
            && (this.getOrderNo() == null ? other.getOrderNo() == null : this.getOrderNo().equals(other.getOrderNo()))
            && (this.getSpuId() == null ? other.getSpuId() == null : this.getSpuId().equals(other.getSpuId()))
            && (this.getSkuId() == null ? other.getSkuId() == null : this.getSkuId().equals(other.getSkuId()))
            && (this.getSkuName() == null ? other.getSkuName() == null : this.getSkuName().equals(other.getSkuName()))
            && (this.getSkuSpec() == null ? other.getSkuSpec() == null : this.getSkuSpec().equals(other.getSkuSpec()))
            && (this.getSkuImage() == null ? other.getSkuImage() == null : this.getSkuImage().equals(other.getSkuImage()))
            && (this.getSkuPrice() == null ? other.getSkuPrice() == null : this.getSkuPrice().equals(other.getSkuPrice()))
            && (this.getQuantity() == null ? other.getQuantity() == null : this.getQuantity().equals(other.getQuantity()))
            && (this.getTotalPrice() == null ? other.getTotalPrice() == null : this.getTotalPrice().equals(other.getTotalPrice()))
            && (this.getCreateTime() == null ? other.getCreateTime() == null : this.getCreateTime().equals(other.getCreateTime()));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getId() == null) ? 0 : getId().hashCode());
        result = prime * result + ((getOrderId() == null) ? 0 : getOrderId().hashCode());
        result = prime * result + ((getOrderNo() == null) ? 0 : getOrderNo().hashCode());
        result = prime * result + ((getSpuId() == null) ? 0 : getSpuId().hashCode());
        result = prime * result + ((getSkuId() == null) ? 0 : getSkuId().hashCode());
        result = prime * result + ((getSkuName() == null) ? 0 : getSkuName().hashCode());
        result = prime * result + ((getSkuSpec() == null) ? 0 : getSkuSpec().hashCode());
        result = prime * result + ((getSkuImage() == null) ? 0 : getSkuImage().hashCode());
        result = prime * result + ((getSkuPrice() == null) ? 0 : getSkuPrice().hashCode());
        result = prime * result + ((getQuantity() == null) ? 0 : getQuantity().hashCode());
        result = prime * result + ((getTotalPrice() == null) ? 0 : getTotalPrice().hashCode());
        result = prime * result + ((getCreateTime() == null) ? 0 : getCreateTime().hashCode());
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append(" [");
        sb.append("Hash = ").append(hashCode());
        sb.append(", id=").append(id);
        sb.append(", orderId=").append(orderId);
        sb.append(", orderNo=").append(orderNo);
        sb.append(", spuId=").append(spuId);
        sb.append(", skuId=").append(skuId);
        sb.append(", skuName=").append(skuName);
        sb.append(", skuSpec=").append(skuSpec);
        sb.append(", skuImage=").append(skuImage);
        sb.append(", skuPrice=").append(skuPrice);
        sb.append(", quantity=").append(quantity);
        sb.append(", totalPrice=").append(totalPrice);
        sb.append(", createTime=").append(createTime);
        sb.append(", serialVersionUID=").append(serialVersionUID);
        sb.append("]");
        return sb.toString();
    }
}