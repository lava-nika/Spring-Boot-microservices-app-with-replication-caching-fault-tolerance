package com.example.order;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@RestController
@RequestMapping("/orders")
public class OrderServiceController {

    private final Map<Integer, Map<String, Object>> orders = new ConcurrentHashMap<>();
    private final AtomicInteger orderCounter = new AtomicInteger(1);
    private static final Logger logger = LoggerFactory.getLogger(OrderServiceController.class);

    @PostMapping
    public ResponseEntity<?> createOrder(@RequestBody Map<String, Object> order) {
        int orderId = orderCounter.getAndIncrement();
        order.put("number", orderId);
        orders.put(orderId, order);

        Map<String, Object> response = new HashMap<>();
        response.put("data", order);
        logger.info("Order created: {}", order);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<?> getOrder(@PathVariable("orderId") int orderId) {
        if (!orders.containsKey(orderId)) {
            Map<String, Object> error = new HashMap<>();
            error.put("code", 404);
            error.put("message", "Order not found");

            Map<String, Object> response = new HashMap<>();
            response.put("error", error);
            logger.warn("Order not found: {}", orderId);
            return ResponseEntity.status(404).body(response);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("data", orders.get(orderId));
        logger.info("Found order #{}: {}", orderId, orders.get(orderId));
        return ResponseEntity.ok(response);
    }
}
