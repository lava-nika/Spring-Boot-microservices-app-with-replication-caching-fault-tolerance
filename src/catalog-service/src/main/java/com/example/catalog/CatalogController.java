package com.example.catalog;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

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

    // thread-safe map to store stock names and the volumes
    private final Map<String, Integer> stockVolume = new ConcurrentHashMap<>();

    // used to make REST calls, eg. for cache invalidation
    private final RestTemplate restTemplate = new RestTemplate();

    // logger support to print logs in a structured manner
    private static final Logger logger = LoggerFactory.getLogger(CatalogController.class);

    // initialize the stockVolume map by reading from stocks.csv
    // this method is called after the controller is constructed
    @PostConstruct
    public void init() throws IOException {
        InputStream is = getClass().getClassLoader().getResourceAsStream("stocks.csv");
        if (is == null) {
            System.err.println("stocks.csv not found in resources!");
            return;
        }
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        String line;

        // skip header line
        reader.readLine();
        while ((line = reader.readLine()) != null) {
            String[] parts = line.split(",");
            if (parts.length >= 2) {
                stockVolume.put(parts[0], Integer.parseInt(parts[1]));
            }
            logger.info("Loaded stock: {} with volume: {}", parts[0], parts[1]);
        }
    }

    // return volume for a given stock name, API endpoint is GET /stocks/<stockname>
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

        // if stock found, return name and current volume
        Map<String, Object> data = new HashMap<>();
        data.put("name", stockName);
        data.put("volume", stockVolume.get(stockName));
        return ResponseEntity.ok(data);
    }

    // handle stock trade requests like buy/sell, API endpoint is POST /stocks/trade
    @PostMapping("/trade")
    public ResponseEntity<?> tradeStock(@RequestBody Map<String, Object> request) {
        String stockName = (String) request.get("name");
        String type = (String) request.get("type");
        int quantity = (int) request.get("quantity");

        if (!stockVolume.containsKey(stockName)) {
            // stock not found
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
            // because buying decreases volume
            if (quantity > currentVolume) {
                // not enough stock available for the request
                logger.warn("BUY failed! insufficient stock: {} (requested = {}, available = {})", stockName, quantity, currentVolume);
                Map<String, Object> error = new HashMap<>();
                error.put("code", 400);
                error.put("message", "Not enough stock available");

                Map<String, Object> response = new HashMap<>();
                response.put("error", error);

                return ResponseEntity.status(400).body(response);
            }
            stockVolume.put(stockName, currentVolume - quantity);
            logger.info("BUY: {} of {}; remaining volume = {}", quantity, stockName, stockVolume.get(stockName));
        } else if ("sell".equalsIgnoreCase(type)) {
            // because selling increases volume
            stockVolume.put(stockName, currentVolume + quantity);
            logger.info("SELL: {} of {}; new total = {}", quantity, stockName, stockVolume.get(stockName));
        } else {
            // invalid trade type (not buy or sell)
            logger.error("Invalid trade type: {}", type);
            Map<String, Object> error = new HashMap<>();
            error.put("code", 400);
            error.put("message", "Invalid trade type");

            Map<String, Object> response = new HashMap<>();
            response.put("error", error);

            return ResponseEntity.status(400).body(response);
        }

        // tell frontend to invalidate cache for this stock
        try {
            String frontendUrl = System.getenv().getOrDefault("FRONTEND_URL", "http://localhost:7070");
            restTemplate.postForEntity(frontendUrl + "/stocks/invalidate/" + stockName, null, String.class);
            logger.info("Cache invalidation triggered for {}", stockName);
        } catch (Exception e) {
            logger.error("Cache invalidation failed for {}: {}", stockName, e.getMessage());
        }
        // if trade is successful
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Trade successful");
        return ResponseEntity.ok(response);
    }
}
