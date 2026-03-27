package com.example.mini_marketplace.dto;

import com.example.mini_marketplace.entity.Order;
import com.example.mini_marketplace.entity.OrderItem;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
public class BuyerOrderView {

    private Long orderId;
    private Order.Status status;
    private LocalDateTime createdAt;
    private BigDecimal totalAmount;
    private Order.PaymentMethod paymentMethod;
    private String paymentReference;
    private List<ItemView> items;

    public static BuyerOrderView from(Order order) {
        BuyerOrderView view = new BuyerOrderView();
        view.setOrderId(order.getId());
        view.setStatus(order.getStatus());
        view.setCreatedAt(order.getCreatedAt());
        view.setTotalAmount(order.getTotalAmount());
        view.setPaymentMethod(order.getPaymentMethod());
        view.setPaymentReference(order.getPaymentReference());

        view.setItems(order.getItems().stream()
                .map(ItemView::from)
                .collect(Collectors.toList()));
        return view;
    }

    @Getter
    @Setter
    public static class ItemView {
        private Long productId;
        private String productName;
        private String productImageUrl;
        private String sellerUsername;
        private Integer quantity;
        private BigDecimal unitPrice;
        private BigDecimal subtotal;

        public static ItemView from(OrderItem oi) {
            ItemView v = new ItemView();
            v.setProductId(oi.getProduct().getId());
            v.setProductName(oi.getProduct().getName());
            v.setProductImageUrl(oi.getProduct().getImageUrl());
            v.setSellerUsername(oi.getProduct().getSeller().getUsername());
            v.setQuantity(oi.getQuantity());
            v.setUnitPrice(oi.getUnitPrice());
            v.setSubtotal(oi.getUnitPrice().multiply(BigDecimal.valueOf(oi.getQuantity())));
            return v;
        }
    }
}
