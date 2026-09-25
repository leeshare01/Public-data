package com.eshop.order.controller;

import com.eshop.common.dto.PageParam;
import com.eshop.common.dto.PageResult;
import com.eshop.common.result.Result;
import com.eshop.common.result.ResultCode;
import com.eshop.order.entity.Order;
import com.eshop.order.entity.OrderItem;
import com.eshop.order.service.OrderService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/order")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    /** 创建订单 */
    @PostMapping("/orders")
    public Result<Map<String, Object>> create(@RequestHeader("X-User-Id") Long userId,
                                               @RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> itemsMap = (List<Map<String, Object>>) body.get("items");
        String consigneeName = (String) body.get("consigneeName");
        String consigneePhone = (String) body.get("consigneePhone");
        String consigneeAddress = (String) body.get("consigneeAddress");
        String remark = (String) body.get("remark");

        List<OrderItem> items = itemsMap.stream().map(m -> {
            OrderItem item = new OrderItem();
            item.setProductId(Long.valueOf(m.get("productId").toString()));
            if (m.containsKey("skuId") && m.get("skuId") != null) {
                item.setSkuId(Long.valueOf(m.get("skuId").toString()));
            }
            item.setProductName((String) m.get("productName"));
            item.setProductImage((String) m.get("productImage"));
            if (m.containsKey("specValues") && m.get("specValues") != null) {
                item.setSpecValues((String) m.get("specValues"));
            }
            item.setPrice(new java.math.BigDecimal(m.get("price").toString()));
            item.setQuantity(Integer.valueOf(m.get("quantity").toString()));
            return item;
        }).collect(Collectors.toList());

        return Result.success("下单成功",
                orderService.create(userId, consigneeName, consigneePhone, consigneeAddress, remark, items));
    }

    /** 订单列表 */
    @GetMapping("/orders")
    public Result<PageResult<Order>> list(@RequestHeader("X-User-Id") Long userId,
                                          @RequestParam(defaultValue = "1") int page,
                                          @RequestParam(defaultValue = "10") int size,
                                          @RequestParam(required = false) Integer status) {
        PageParam pageParam = new PageParam();
        pageParam.setPage(page);
        pageParam.setSize(size);
        return Result.success(orderService.page(userId, pageParam, status));
    }

    /** 订单详情 */
    @GetMapping("/orders/{id}")
    public Result<Order> detail(@RequestHeader("X-User-Id") Long userId,
                                 @PathVariable Long id) {
        return Result.success(orderService.getDetail(userId, id));
    }

    /** 取消订单 */
    @PutMapping("/orders/{id}/cancel")
    public Result<Void> cancel(@RequestHeader("X-User-Id") Long userId,
                                @PathVariable Long id,
                                @RequestBody Map<String, String> body) {
        orderService.cancel(userId, id, body.get("reason"));
        return Result.success("订单已取消", null);
    }

    /** 模拟支付 */
    @PutMapping("/orders/{id}/pay")
    public Result<Map<String, Object>> pay(@RequestHeader("X-User-Id") Long userId,
                                            @PathVariable Long id) {
        return Result.success("支付成功", orderService.pay(userId, id));
    }

    /** 确认收货 */
    @PutMapping("/orders/{id}/confirm-receive")
    public Result<Void> confirmReceive(@RequestHeader("X-User-Id") Long userId,
                                        @PathVariable Long id) {
        orderService.confirmReceive(userId, id);
        return Result.success("已确认收货", null);
    }

    // ========== 管理员接口 ==========

    /** 管理员 — 发货 */
    @PutMapping("/admin/orders/{id}/deliver")
    public Result<Void> deliver(@RequestHeader("X-User-Id") Long userId,
                                 @RequestHeader(value = "X-Role", required = false) String role,
                                 @PathVariable Long id) {
        if (!"ADMIN".equals(role)) return Result.error(ResultCode.FORBIDDEN, "无管理员权限");
        orderService.deliver(id);
        return Result.success("已发货", null);
    }

    /** 管理员 — 全部订单列表 */
    @GetMapping("/admin/orders")
    public Result<PageResult<Order>> adminList(@RequestHeader(value = "X-Role", required = false) String role,
                                                @RequestParam(defaultValue = "1") int page,
                                                @RequestParam(defaultValue = "20") int size,
                                                @RequestParam(required = false) Integer status) {
        if (!"ADMIN".equals(role)) return Result.error(ResultCode.FORBIDDEN, "无管理员权限");
        PageParam pageParam = new PageParam();
        pageParam.setPage(page);
        pageParam.setSize(size);
        return Result.success(orderService.adminPage(pageParam, status));
    }
}
