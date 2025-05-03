package com.example.order;

import jakarta.annotation.PostConstruct;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
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

    @Value("${replica.id}")
    private int replicaId;

    @Value("#{'${replica.all}'.split(',')}")
    private List<String> allReplicas;

    @Value("${server.port}")
    private int serverPort;

    private int getServerPort() {
        return serverPort;
    }

    private final RestTemplate restTemplate = new RestTemplate();

    @PostConstruct
    public void init() {
        logger.info("Replica started with ID: {}", replicaId);
    }

    @PostMapping
    public ResponseEntity<?> createOrder(@RequestBody Map<String, Object> order) {
        int orderId = orderCounter.getAndIncrement();
        order.put("number", orderId);
        orders.put(orderId, order);
        logger.info("Order created: {}", order);

        // Propagate to followers if it is leader
        int currentLeaderId = -1;
        String currentLeaderUrl = null;

        for (String url : allReplicas) {
            try {
                Map<?, ?> res = restTemplate.getForObject(url + "/orders/ping", Map.class);
                int id = ((Number) res.get("replicaId")).intValue();
                if (id > currentLeaderId) {
                    currentLeaderId = id;
                    currentLeaderUrl = url;
                }
            } catch (Exception e) {
                logger.warn("Replica unreachable: {}", url);
            }
        }

        if (replicaId == currentLeaderId) {
            logger.info("This replica (ID {}) is the leader. Propagating order.", replicaId);
            for (String url : allReplicas) {
                if (!url.contains(":" + getServerPort())) {  // avoid self-propagation
                    try {
                        restTemplate.postForEntity(url + "/orders/propagate", order, String.class);
                        logger.info("Propagated to {}", url);
                    } catch (Exception e) {
                        logger.warn("Failed to propagate to {}: {}", url, e.getMessage());
                    }
                }
            }
        } else {
            logger.info("This replica (ID {}) is not the leader (ID {}). Skipping propagation.", replicaId, currentLeaderId);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("data", order);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/propagate")
    public ResponseEntity<?> replicateOrder(@RequestBody Map<String, Object> order) {
        int orderId = (int) order.get("number");
        orders.put(orderId, order);
        logger.info("Order replicated: {}", order);
        return ResponseEntity.ok().build();
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

    @GetMapping("/ping")
    public ResponseEntity<?> ping() {
        Map<String, Object> response = new HashMap<>();
        response.put("replicaId", replicaId);
        response.put("status", "alive");
        return ResponseEntity.ok(response);
    }

}
