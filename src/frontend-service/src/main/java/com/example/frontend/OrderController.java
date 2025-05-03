package com.example.frontend;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/orders")
public class OrderController {

    @Autowired
    private OrderLeaderSelector leaderSelector;

//    @Value("${order.leader.url:http://localhost:9091}")
//    private String orderServiceUrl;



    private final RestTemplate restTemplate = new RestTemplate();
    private static final Logger logger = LoggerFactory.getLogger(OrderController.class);

    @PostMapping
    public ResponseEntity<?> createOrder(@RequestBody Map<String, Object> order) {
        logger.info("Forwarding order to order service: {}", order);
        String leaderUrl = leaderSelector.getLeader();

        try {
//            return restTemplate.postForEntity(orderServiceUrl + "/orders", order, Object.class);
            return restTemplate.postForEntity(leaderUrl + "/orders", order, Object.class);
        } catch (Exception e) {
            logger.error("Leader unreachable, resetting");
            leaderSelector.resetLeader();
            Map<String, Object> error = new HashMap<>();
            error.put("code", 500);
            error.put("message", "Order service leader unreachable");

            Map<String, Object> response = new HashMap<>();
            response.put("error", error);
            return ResponseEntity.status(500).body(response);
        }
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<?> getOrder(@PathVariable("orderId") int orderId) {
        String leaderUrl = leaderSelector.getLeader();
        logger.info("Requesting order #{} from order service", orderId);
        try {
//            return restTemplate.getForEntity(orderServiceUrl + "/orders/" + orderId, Object.class);
            return restTemplate.getForEntity(leaderUrl + "/orders/" + orderId, Object.class);
        } catch (Exception e) {
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
