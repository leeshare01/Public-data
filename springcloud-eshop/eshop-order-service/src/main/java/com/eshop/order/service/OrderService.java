package com.eshop.order.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.eshop.common.dto.PageParam;
import com.eshop.common.dto.PageResult;
import com.eshop.common.exception.BusinessException;
import com.eshop.common.result.ResultCode;
import com.eshop.order.entity.Order;
import com.eshop.order.entity.OrderItem;
import com.eshop.order.feign.CartClient;
import com.eshop.order.entity.StatusLog;
import com.eshop.order.mapper.OrderItemMapper;
import com.eshop.order.mapper.OrderMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 订单服务
 */
@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final CartClient cartClient;

    public OrderService(OrderMapper orderMapper, OrderItemMapper orderItemMapper,
                        CartClient cartClient) {
        this.orderMapper = orderMapper;
        this.orderItemMapper = orderItemMapper;
        this.cartClient = cartClient;
    }

    /**
     * 创建订单
     */
    @Transactional
    public Map<String, Object> create(Long userId, String consigneeName, String consigneePhone,
                                       String consigneeAddress, String remark,
                                       List<OrderItem> items) {
        if (items == null || items.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "订单商品不能为空");
        }

        // 生成订单号
        String orderNo = generateOrderNo();
        BigDecimal totalAmount = items.stream()
                .map(i -> i.getPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 创建订单
        Order order = new Order();
        order.setOrderNo(orderNo);
        order.setUserId(userId);
        order.setTotalAmount(totalAmount);
        order.setPayAmount(totalAmount);
        order.setStatus(0); // 待付款
        order.setConsigneeName(consigneeName);
        order.setConsigneePhone(consigneePhone);
        order.setConsigneeAddress(consigneeAddress);
        order.setRemark(remark != null ? remark : "");
        orderMapper.insert(order);

        // 保存订单明细
        for (OrderItem item : items) {
            item.setOrderNo(orderNo);
            item.setSubtotal(item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
            orderItemMapper.insert(item);
        }

        // 清空购物车（Feign 调用 cart-service）
        try {
            cartClient.clear(userId);
        } catch (Exception e) {
            log.warn("清空购物车失败(不影响订单): {}", e.getMessage());
        }

        return Map.of(
                "orderId", order.getId(),
                "orderNo", orderNo,
                "payAmount", totalAmount,
                "status", 0
        );
    }

    /**
     * 分页查询订单
     */
    public PageResult<Order> page(Long userId, PageParam pageParam, Integer status) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<Order>()
                .eq(Order::getUserId, userId);
        if (status != null) {
            wrapper.eq(Order::getStatus, status);
        }
        wrapper.orderByDesc(Order::getCreateTime);

        IPage<Order> page = orderMapper.selectPage(
                new Page<>(pageParam.getPage(), pageParam.getSize()), wrapper);

        List<Order> records = page.getRecords().stream().peek(this::enrichOrder).collect(Collectors.toList());
        return PageResult.of(records, page.getTotal(), (int) page.getCurrent(), (int) page.getSize());
    }

    /**
     * 查询订单详情
     */
    public Order getDetail(Long userId, Long orderId) {
        Order order = orderMapper.selectOne(
                new LambdaQueryWrapper<Order>().eq(Order::getId, orderId).eq(Order::getUserId, userId));
        if (order == null) {
            throw new BusinessException(ResultCode.ORDER_NOT_FOUND);
        }
        enrichOrder(order);
        return order;
    }

    /**
     * 模拟支付（状态 0 → 1）
     */
    @Transactional
    public Map<String, Object> pay(Long userId, Long orderId) {
        Order order = orderMapper.selectOne(
                new LambdaQueryWrapper<Order>().eq(Order::getId, orderId).eq(Order::getUserId, userId));
        if (order == null) {
            throw new BusinessException(ResultCode.ORDER_NOT_FOUND);
        }
        if (order.getStatus() != 0) {
            throw new BusinessException(ResultCode.ORDER_STATUS_ERROR, "当前状态不允许支付");
        }
        order.setStatus(1);
        orderMapper.updateById(order);
        return Map.of("orderId", order.getId(), "status", 1);
    }

    /**
     * 取消订单
     */
    @Transactional
    public void cancel(Long userId, Long orderId, String reason) {
        Order order = orderMapper.selectOne(
                new LambdaQueryWrapper<Order>().eq(Order::getId, orderId).eq(Order::getUserId, userId));
        if (order == null) {
            throw new BusinessException(ResultCode.ORDER_NOT_FOUND);
        }
        if (order.getStatus() != 0) {
            throw new BusinessException(ResultCode.ORDER_STATUS_ERROR, "当前状态不允许取消");
        }
        order.setStatus(4);
        orderMapper.updateById(order);
    }

    /**
     * 确认收货
     */
    @Transactional
    public void confirmReceive(Long userId, Long orderId) {
        Order order = orderMapper.selectOne(
                new LambdaQueryWrapper<Order>().eq(Order::getId, orderId).eq(Order::getUserId, userId));
        if (order == null) {
            throw new BusinessException(ResultCode.ORDER_NOT_FOUND);
        }
        if (order.getStatus() != 2) {
            throw new BusinessException(ResultCode.ORDER_STATUS_ERROR, "当前状态不允许确认收货");
        }
        order.setStatus(3);
        orderMapper.updateById(order);
    }

    // ========== 管理员接口 ==========

    /**
     * 管理员发货
     */
    @Transactional
    public void deliver(Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(ResultCode.ORDER_NOT_FOUND);
        }
        if (order.getStatus() != 1) {
            throw new BusinessException(ResultCode.ORDER_STATUS_ERROR, "当前状态不允许发货");
        }
        order.setStatus(2);
        orderMapper.updateById(order);
    }

    /**
     * 管理员查询全部订单（分页）
     */
    public PageResult<Order> adminPage(PageParam pageParam, Integer status) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            wrapper.eq(Order::getStatus, status);
        }
        wrapper.orderByDesc(Order::getCreateTime);

        IPage<Order> page = orderMapper.selectPage(
                new Page<>(pageParam.getPage(), pageParam.getSize()), wrapper);

        List<Order> records = page.getRecords().stream().peek(this::enrichOrder).collect(Collectors.toList());
        return PageResult.of(records, page.getTotal(), (int) page.getCurrent(), (int) page.getSize());
    }

    /**
     * 填充订单附加信息
     */
    private void enrichOrder(Order order) {
        order.setStatusText(getStatusText(order.getStatus()));

        // 查询订单明细
        List<OrderItem> items = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderNo, order.getOrderNo()));
        order.setItems(items);

        // 构造时间线
        List<StatusLog> timeline = new ArrayList<>();
        timeline.add(StatusLog.of(order.getStatus(),
                formatTime(order.getCreateTime()), getStatusDesc(order.getStatus())));
        order.setStatusTimeline(timeline);
    }

    private String getStatusText(int status) {
        return switch (status) {
            case 0 -> "待付款";
            case 1 -> "已付款";
            case 2 -> "已发货";
            case 3 -> "已完成";
            case 4 -> "已取消";
            default -> "未知";
        };
    }

    private String getStatusDesc(int status) {
        return switch (status) {
            case 0 -> "订单创建";
            case 1 -> "订单已付款";
            case 2 -> "订单已发货";
            case 3 -> "订单已完成";
            case 4 -> "订单已取消";
            default -> "";
        };
    }

    private String formatTime(LocalDateTime time) {
        if (time == null) return "";
        return time.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    /**
     * 生成订单号：时间戳 + 随机6位
     */
    private String generateOrderNo() {
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String random = String.format("%06d", new Random().nextInt(999999));
        return date + random;
    }
}
