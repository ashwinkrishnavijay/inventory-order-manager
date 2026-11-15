package com.inventory.manager.service;

import com.inventory.manager.dto.request.OrderItemRequest;
import com.inventory.manager.dto.request.OrderRequest;
import com.inventory.manager.dto.response.OrderItemResponse;
import com.inventory.manager.dto.response.OrderResponse;
import com.inventory.manager.dto.response.PageResponse;
import com.inventory.manager.entity.*;
import com.inventory.manager.exception.BadRequestException;
import com.inventory.manager.exception.InsufficientStockException;
import com.inventory.manager.exception.ResourceNotFoundException;
import com.inventory.manager.repository.OrderRepository;
import com.inventory.manager.repository.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final AuthService authService;

    public OrderService(OrderRepository orderRepository,
                        ProductRepository productRepository,
                        AuthService authService) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.authService = authService;
    }

    @Transactional
    public OrderResponse placeOrder(OrderRequest request) {
        User user = authService.getCurrentUser();
        Order order = new Order(user, OrderStatus.CREATED);

        BigDecimal total = BigDecimal.ZERO;

        for (OrderItemRequest itemReq : request.getItems()) {
            Product product = productRepository.findById(itemReq.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + itemReq.getProductId()));

            if (product.getStatus() != ProductStatus.ACTIVE) {
                throw new BadRequestException("Product '" + product.getName() + "' is not active for ordering");
            }

            // Check stock
            if (product.getStockQuantity() < itemReq.getQuantity()) {
                throw new InsufficientStockException(
                        "Product '" + product.getName() + "' has insufficient stock. Available: "
                                + product.getStockQuantity() + ", Requested: " + itemReq.getQuantity()
                );
            }

            // Deduct stock automatically
            product.setStockQuantity(product.getStockQuantity() - itemReq.getQuantity());
            productRepository.save(product);

            OrderItem orderItem = new OrderItem(order, product, itemReq.getQuantity(), product.getPrice());
            order.addOrderItem(orderItem);
            total = total.add(orderItem.getSubtotal());
        }

        order.setTotalAmount(total);
        order.setStatus(OrderStatus.CONFIRMED); // Order confirmed with stock deducted

        Order saved = orderRepository.save(order);
        return mapToResponse(saved);
    }

    @Transactional
    public OrderResponse cancelOrder(Long orderId) {
        User user = authService.getCurrentUser();
        Order order;

        if (user.getRole() == Role.ADMIN) {
            order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new ResourceNotFoundException("Order not found with ID: " + orderId));
        } else {
            order = orderRepository.findByIdAndUserId(orderId, user.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Order not found with ID: " + orderId));
        }

        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new BadRequestException("Order is already cancelled");
        }

        // Restore stock when cancelled
        for (OrderItem item : order.getOrderItems()) {
            Product product = item.getProduct();
            product.setStockQuantity(product.getStockQuantity() + item.getQuantity());
            productRepository.save(product);
        }

        order.setStatus(OrderStatus.CANCELLED);
        Order updated = orderRepository.save(order);
        return mapToResponse(updated);
    }

    @Transactional(readOnly = true)
    public PageResponse<OrderResponse> getUserOrders(Pageable pageable) {
        User user = authService.getCurrentUser();
        Page<Order> page = orderRepository.findByUserId(user.getId(), pageable);
        return PageResponse.from(page.map(this::mapToResponse));
    }

    @Transactional(readOnly = true)
    public PageResponse<OrderResponse> getAllOrders(Pageable pageable) {
        Page<Order> page = orderRepository.findAll(pageable);
        return PageResponse.from(page.map(this::mapToResponse));
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long orderId) {
        User user = authService.getCurrentUser();
        Order order;
        if (user.getRole() == Role.ADMIN) {
            order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new ResourceNotFoundException("Order not found with ID: " + orderId));
        } else {
            order = orderRepository.findByIdAndUserId(orderId, user.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Order not found with ID: " + orderId));
        }
        return mapToResponse(order);
    }

    private OrderResponse mapToResponse(Order order) {
        List<OrderItemResponse> itemResponses = order.getOrderItems().stream()
                .map(item -> new OrderItemResponse(
                        item.getId(),
                        item.getProduct().getId(),
                        item.getProduct().getName(),
                        item.getQuantity(),
                        item.getUnitPrice(),
                        item.getSubtotal()
                ))
                .collect(Collectors.toList());

        return new OrderResponse(
                order.getId(),
                order.getUser().getId(),
                order.getUser().getName(),
                order.getUser().getEmail(),
                order.getTotalAmount(),
                order.getStatus(),
                itemResponses,
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }
}
