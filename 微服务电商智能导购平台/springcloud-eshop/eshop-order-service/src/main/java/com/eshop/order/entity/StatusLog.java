package com.eshop.order.entity;

/**
 * 订单状态日志（非数据库实体，仅用于返回前端时间线）
 */
public class StatusLog {

    private Integer status;
    private String time;
    private String desc;

    public static StatusLog of(Integer status, String time, String desc) {
        StatusLog log = new StatusLog();
        log.setStatus(status);
        log.setTime(time);
        log.setDesc(desc);
        return log;
    }

    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }
    public String getDesc() { return desc; }
    public void setDesc(String desc) { this.desc = desc; }
}
