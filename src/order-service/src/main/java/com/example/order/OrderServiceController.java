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

// controller for handling order creation, retrieval, replication
// also sync among replicated OrderService instances
@RestController
@RequestMapping("/orders")
public class OrderServiceController {

    // store orders using their orderId
    private final Map<Integer, Map<String, Object>> orders = new ConcurrentHashMap<>();

    // counter to assign unique IDs to new orders
    private final AtomicInteger orderCounter = new AtomicInteger(1);
    private static final Logger logger = LoggerFactory.getLogger(OrderServiceController.class);

    // replica ID get via application.properties
    @Value("${replica.id}")
    private int replicaId;

    // list of all replica urls for leader detection and replication
    @Value("#{'${replica.all}'.split(',')}")
    private List<String> allReplicas;

    // current server port
    // need this to avoid self-replication
    @Value("${server.port}")
    private int serverPort;

    // helper function for retrieving port
    private int getServerPort() {
        return serverPort;
    }

    // for HTTP requests to other replicas
    private final RestTemplate restTemplate = new RestTemplate();

    // log replica ID and if it is a follower, sync orders from leader
    @PostConstruct
    public void init() {
        logger.info("Replica started with ID: {}", replicaId);
        syncWithLeader();
    }


    // create new order with its ID
    // if the replica is leader, replicate the order to followers
    @PostMapping
    public ResponseEntity<?> createOrder(@RequestBody Map<String, Object> order) {
        int orderId = orderCounter.getAndIncrement();
        order.put("number", orderId);
        orders.put(orderId, order);
        logger.info("Order created: {}", order);

        // find leader
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

        // leader alone should replicate to others
        if (replicaId == currentLeaderId) {
            logger.info("This replica (ID {}) is the leader. Propagating order.", replicaId);
            for (String url : allReplicas) {
                if (!url.contains(":" + getServerPort())) {  // to avoid self-propagation
                    try {
                        restTemplate.postForEntity(url + "/orders/replicate", order, String.class);
                        logger.info("Replicated to {}", url);
                    } catch (Exception e) {
                        logger.warn("Failed to replicate to {}: {}", url, e.getMessage());
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

    // leader will use this to replicate order to follower
    // API endpoint is POST /orders/replicate
    @PostMapping("/replicate")
    public ResponseEntity<?> replicateOrder(@RequestBody Map<String, Object> order) {
        int orderId = (int) order.get("number");
        orders.put(orderId, order);
        logger.info("Order replicated: {}", order);
        return ResponseEntity.ok().build();
    }

    // retrieve order by ID. API endpoint is GET /orders/<orderId>
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


    // this is used by frontend, other replicas to check if they are alive
    // API endpoint is GET /orders/ping
    @GetMapping("/ping")
    public ResponseEntity<?> ping() {
        Map<String, Object> response = new HashMap<>();
        response.put("replicaId", replicaId);
        response.put("status", "alive");
        return ResponseEntity.ok(response);
    }

    // returns all orders stored by current replica
    // used by new replicas to sync their state from current leader
    // API endpoint is GET /orders/sync
    @GetMapping("/sync")
    public ResponseEntity<?> syncOrders() {
        Map<String, Object> response = new HashMap<>();
        // send the order values
        response.put("orders", orders.values());
        logger.info("received sync request, sending {} orders", orders.size());
        return ResponseEntity.ok(response);
    }

    // sync orders from current leader
    // triggered if the replica is not leader (on startup)
    private void syncWithLeader() {
        int currentLeaderId = -1;
        String currentLeaderUrl = null;

        for (String url : allReplicas) {
            try {
                Map<?, ?> res = restTemplate.getForObject(url + "/orders/ping", Map.class);
                int id = (int) res.get("replicaId");
                if (id > currentLeaderId) {
                    currentLeaderId = id;
                    currentLeaderUrl = url;
                }
            } catch (Exception e) {
                logger.warn("Replica unreachable during sync check {}", url);
            }
        }

        // only sync if it is not the leader
        if (replicaId != currentLeaderId && currentLeaderUrl != null) {
            try {
                Map<?, ?> res = restTemplate.getForObject(currentLeaderUrl + "/orders/sync", Map.class);
                List<Map<String, Object>> syncedOrders = (List<Map<String, Object>>) res.get("orders");
                for (Map<String, Object> order : syncedOrders) {
                    int id = (int) order.get("number");
                    orders.put(id, order);
                }
                logger.info("Recovered {} orders from leader {}", syncedOrders.size(), currentLeaderUrl);
            } catch (Exception e) {
                logger.warn("Failed to sync from leader {}, {}", currentLeaderUrl, e.getMessage());
            }
        }
    }

}
