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
public class BuyerOrderView {

    private Long orderId;
    private Order.Status status;
    private LocalDateTime createdAt;
    private BigDecimal totalAmount;
    private List<ItemView> items;

    @Getter
    @Setter
    public static class ItemView {
        private Long productId;
        private String productName;
        private String sellerUsername;
        private Integer quantity;
        private BigDecimal unitPrice;
        private BigDecimal subtotal;

        public static ItemView from(OrderItem oi) {
            ItemView v = new ItemView();
            v.setProductId(oi.getProduct().getId());
            v.setProductName(oi.getProduct().getName());
            v.setSellerUsername(oi.getProduct().getSeller().getUsername());
            v.setQuantity(oi.getQuantity());
            v.setUnitPrice(oi.getUnitPrice());
            v.setSubtotal(oi.getUnitPrice().multiply(BigDecimal.valueOf(oi.getQuantity())));
            return v;
        }
    }
}
