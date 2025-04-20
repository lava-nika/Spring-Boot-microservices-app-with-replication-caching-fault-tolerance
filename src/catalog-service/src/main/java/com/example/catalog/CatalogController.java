package com.example.catalog;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

//import javax.annotation.PostConstruct;
import jakarta.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/stocks")
public class CatalogController {

    private final Map<String, Integer> stockVolume = new ConcurrentHashMap<>();
    private final RestTemplate restTemplate = new RestTemplate();
    private static final Logger logger = LoggerFactory.getLogger(CatalogController.class);

    @PostConstruct
    public void init() throws IOException {
        InputStream is = getClass().getClassLoader().getResourceAsStream("stocks.csv");
        if (is == null) {
            System.err.println("stocks.csv not found in resources!");
            return;
        }
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        String line;
        reader.readLine(); // skip header
        while ((line = reader.readLine()) != null) {
            String[] parts = line.split(",");
            if (parts.length >= 2) {
                stockVolume.put(parts[0], Integer.parseInt(parts[1]));
            }
            logger.info("Loaded stock: {} with volume: {}", parts[0], parts[1]);
        }
    }

    @GetMapping("/{stockName}")
    public ResponseEntity<?> getStock(@PathVariable("stockName") String stockName) {
        if (!stockVolume.containsKey(stockName)) {
            Map<String, Object> error = new HashMap<>();
            error.put("code", 404);
            error.put("message", "Stock not found");

            Map<String, Object> response = new HashMap<>();
            response.put("error", error);

            return ResponseEntity.status(404).body(response);
        }

        Map<String, Object> data = new HashMap<>();
        data.put("name", stockName);
        data.put("volume", stockVolume.get(stockName));
        return ResponseEntity.ok(data);
    }

    @PostMapping("/trade")
    public ResponseEntity<?> tradeStock(@RequestBody Map<String, Object> request) {
        String stockName = (String) request.get("name");
        String type = (String) request.get("type");
        int quantity = (int) request.get("quantity");

        if (!stockVolume.containsKey(stockName)) {
            Map<String, Object> error = new HashMap<>();
            error.put("code", 404);
            error.put("message", "Stock not found");

            Map<String, Object> response = new HashMap<>();
            response.put("error", error);
            logger.warn("Trade failed, Stock {} not found", stockName);

            return ResponseEntity.status(404).body(response);
        }

        int currentVolume = stockVolume.get(stockName);
        if ("buy".equalsIgnoreCase(type)) {
            if (quantity > currentVolume) {
                logger.warn("BUY failed, Insufficient stock: {} (requested {}, available {})", stockName, quantity, currentVolume);
                Map<String, Object> error = new HashMap<>();
                error.put("code", 400);
                error.put("message", "Not enough stock available");

                Map<String, Object> response = new HashMap<>();
                response.put("error", error);

                return ResponseEntity.status(400).body(response);
            }
            stockVolume.put(stockName, currentVolume - quantity);
            logger.info("BUY: {} of {}; remaining volume= {}", quantity, stockName, stockVolume.get(stockName));
        } else if ("sell".equalsIgnoreCase(type)) {
            stockVolume.put(stockName, currentVolume + quantity);
            logger.info("SELL: {} of {}; new total= {}", quantity, stockName, stockVolume.get(stockName));
        } else {
            logger.error("Invalid trade type: {}", type);
            Map<String, Object> error = new HashMap<>();
            error.put("code", 400);
            error.put("message", "Invalid trade type");

            Map<String, Object> response = new HashMap<>();
            response.put("error", error);

            return ResponseEntity.status(400).body(response);
        }

        // Trigger cache invalidation via frontend
        try {
            String frontendUrl = System.getenv().getOrDefault("FRONTEND_URL", "http://localhost:7070");
            restTemplate.postForEntity(frontendUrl + "/stocks/invalidate/" + stockName, null, String.class);
            logger.info("Cache invalidation triggered for {}", stockName);
        } catch (Exception e) {
            logger.error("Cache invalidation failed for {}: {}", stockName, e.getMessage());
            //System.err.println("Cache invalidation failed: " + e.getMessage());
        }

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Trade successful");
        return ResponseEntity.ok(response);
    }
}
