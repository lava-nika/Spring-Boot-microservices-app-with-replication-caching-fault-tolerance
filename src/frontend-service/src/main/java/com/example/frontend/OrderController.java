package com.example.frontend;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// handles order-related API endpoints on the frontend
// forwards order requests to the current leader replica of the order service
// in case of failure, retries using OrderLeaderSelector

@RestController
@RequestMapping("/orders")
public class OrderController {

    @Autowired
    private OrderLeaderSelector leaderSelector;

    private final RestTemplate restTemplate = new RestTemplate();
    private static final Logger logger = LoggerFactory.getLogger(OrderController.class);

    // handle order creation requests, forwards the request to current leader replica
    // if leader is unreachable, it resets the leader and retries with next available replica
    @PostMapping
    public ResponseEntity<?> createOrder(@RequestBody Map<String, Object> order) {
        logger.info("Forwarding order to order service: {}", order);
        String leaderUrl = leaderSelector.getLeader();

        try {
            // try to send the order to current leader
            ResponseEntity<Map> response = restTemplate.postForEntity(leaderUrl + "/orders", order, Map.class);
            return ResponseEntity.ok(response.getBody());
        } catch (Exception e) {
            // when leader fails, try to reset and retry with a new leader
            logger.error("Leader unreachable, resetting");
            leaderSelector.resetLeader();
            String newLeader = leaderSelector.getLeader();
            if (newLeader == null) {
                logger.error("No reachable leader after retry.");
                Map<String, Object> error = new HashMap<>();
                error.put("code", 500);
                error.put("message", "All order replicas are unreachable");

                Map<String, Object> response = new HashMap<>();
                response.put("error", error);
                return ResponseEntity.status(500).body(response);
            }

            try {
                logger.info("Retrying order with new leader: {}", newLeader);
                ResponseEntity<Map> retryResponse = restTemplate.postForEntity(newLeader + "/orders", order, Map.class);
                return ResponseEntity.ok(retryResponse.getBody());
            } catch (Exception retryEx) {
                // final failure after retry
                logger.error("Retry with new leader {} failed: {}", newLeader, retryEx.getMessage());
                Map<String, Object> error = new HashMap<>();
                error.put("code", 500);
                error.put("message", "Order failed after retrying to switch the leader");

                Map<String, Object> response = new HashMap<>();
                response.put("error", error);
                return ResponseEntity.status(500).body(response);
            }
        }
    }

    // retrieves order by its ID from current order service leader
    // API endpoint is GET /orders/<orderId>
    @GetMapping("/{orderId}")
    public ResponseEntity<?> getOrder(@PathVariable("orderId") int orderId) {
        String leaderUrl = leaderSelector.getLeader();
        logger.info("Requesting order #{} from order service", orderId);
        try {
            return restTemplate.getForEntity(leaderUrl + "/orders/" + orderId, Object.class);
        } catch (Exception e) {
            // failed to reach the leader for order retrieval
            logger.error("Failed to get order #{}: {}", orderId, e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("code", 500);
            error.put("message", "Order service leader unreachable");

            Map<String, Object> response = new HashMap<>();
            response.put("error", error);

            return ResponseEntity.status(500).body(response);
        }
    }
}
