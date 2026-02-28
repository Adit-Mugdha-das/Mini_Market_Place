package com.example.mini_marketplace.dto;

import com.example.mini_marketplace.entity.Order;
import com.example.mini_marketplace.entity.OrderItem;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class SellerOrderView {

    private Long orderId;
    private String buyerUsername;
    private Order.Status status;
    private LocalDateTime createdAt;
    // only items that belong to this seller
    private List<ItemView> sellerItems;
    private BigDecimal sellerTotal;

    @Getter
    @Setter
    public static class ItemView {
        private String productName;
        private Integer quantity;
        private BigDecimal unitPrice;
        private BigDecimal subtotal;

        public static ItemView from(OrderItem oi) {
            ItemView v = new ItemView();
            v.setProductName(oi.getProduct().getName());
            v.setQuantity(oi.getQuantity());
            v.setUnitPrice(oi.getUnitPrice());
            v.setSubtotal(oi.getUnitPrice().multiply(BigDecimal.valueOf(oi.getQuantity())));
            return v;
        }
    }
}
