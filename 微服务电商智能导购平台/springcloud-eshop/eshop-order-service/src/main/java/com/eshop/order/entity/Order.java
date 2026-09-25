package com.eshop.order.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单实体
 */
@TableName("orders")
public class Order {

    private Long id;
    private String orderNo;
    private Long userId;
    private BigDecimal totalAmount;
    private BigDecimal payAmount;
    private Integer status;          // 0-待付款 1-已付款 2-已发货 3-已完成 4-已取消
    private String consigneeName;
    private String consigneePhone;
    private String consigneeAddress;
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    /** 非数据库字段 */
    @TableField(exist = false)
    private String statusText;
    @TableField(exist = false)
    private List<OrderItem> items;
    @TableField(exist = false)
    private List<StatusLog> statusTimeline;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getOrderNo() { return orderNo; }
    public void setOrderNo(String orderNo) { this.orderNo = orderNo; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    public BigDecimal getPayAmount() { return payAmount; }
    public void setPayAmount(BigDecimal payAmount) { this.payAmount = payAmount; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public String getConsigneeName() { return consigneeName; }
    public void setConsigneeName(String consigneeName) { this.consigneeName = consigneeName; }
    public String getConsigneePhone() { return consigneePhone; }
    public void setConsigneePhone(String consigneePhone) { this.consigneePhone = consigneePhone; }
    public String getConsigneeAddress() { return consigneeAddress; }
    public void setConsigneeAddress(String consigneeAddress) { this.consigneeAddress = consigneeAddress; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
    public String getStatusText() { return statusText; }
    public void setStatusText(String statusText) { this.statusText = statusText; }
    public List<OrderItem> getItems() { return items; }
    public void setItems(List<OrderItem> items) { this.items = items; }
    public List<StatusLog> getStatusTimeline() { return statusTimeline; }
    public void setStatusTimeline(List<StatusLog> statusTimeline) { this.statusTimeline = statusTimeline; }
}
