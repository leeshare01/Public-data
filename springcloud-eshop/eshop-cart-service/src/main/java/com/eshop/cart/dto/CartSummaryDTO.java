package com.eshop.cart.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * 购物车汇总
 */
public class CartSummaryDTO {

    private List<CartItemDTO> items;
    private BigDecimal totalAmount;
    private int totalCount;

    public static CartSummaryDTO of(List<CartItemDTO> items) {
        CartSummaryDTO dto = new CartSummaryDTO();
        dto.setItems(items);
        dto.setTotalCount(items.stream().mapToInt(CartItemDTO::getQuantity).sum());
        dto.setTotalAmount(items.stream()
                .map(i -> i.getPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        return dto;
    }

    public List<CartItemDTO> getItems() { return items; }
    public void setItems(List<CartItemDTO> items) { this.items = items; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    public int getTotalCount() { return totalCount; }
    public void setTotalCount(int totalCount) { this.totalCount = totalCount; }
}
