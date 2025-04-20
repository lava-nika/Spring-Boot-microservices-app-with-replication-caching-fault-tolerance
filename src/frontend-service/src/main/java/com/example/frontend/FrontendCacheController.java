package com.example.frontend;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

//import javax.annotation.PostConstruct;
import jakarta.annotation.PostConstruct;
import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@RestController
@RequestMapping("/stocks")
public class FrontendCacheController {

    @Value("${cache.size:10}")
    private int cacheSize;

    private Map<String, String> cache;
    private static final Logger logger = LoggerFactory.getLogger(FrontendCacheController.class);

    @PostConstruct
    public void init() {
        cache = new LinkedHashMap<String, String>(cacheSize, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, String> eldest) {
                return size() > cacheSize;
            }
        };
    }

    @GetMapping("/{stockName}")
    public ResponseEntity<String> getStock(@PathVariable("stockName") String stockName) {
        logger.info("Stock lookup request received: {}", stockName);
        synchronized (cache) {
            if (cache.containsKey(stockName)) {
                System.out.println("CACHE HIT for " + stockName);
                logger.info("CACHE HIT: {}", stockName);
                return ResponseEntity.ok(cache.get(stockName));
            }
            else {
                logger.info("CACHE MISS: {}", stockName);
            }
        }

        //System.out.println("CACHE MISS for " + stockName);

        try {
            String catalogUrl = System.getenv().getOrDefault("CATALOG_URL", "http://localhost:8081");
            logger.info("Fetching {} from catalog at {}", stockName, catalogUrl);

            java.net.URL url = new java.net.URL(catalogUrl + "/stocks/" + stockName);
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            int status = conn.getResponseCode();
            java.io.InputStream in = (status == 200) ? conn.getInputStream() : conn.getErrorStream();
            //String response = new String(in.readAllBytes());
            java.util.Scanner scanner = new java.util.Scanner(in).useDelimiter("\\A");
            String response = scanner.hasNext() ? scanner.next() : "";
            in.close();

            if (status == 200) {
                synchronized (cache) {
                    cache.put(stockName, response);
                }
                logger.info("Caching result for {}", stockName);
                return ResponseEntity.ok(response);
            } else {
                logger.warn("Catalog returned error {} for {}", status, stockName);
                return ResponseEntity.status(status).body(response);
            }
        } catch (Exception e) {
            logger.error("Error fetching stock {} from catalog: {}", stockName, e.getMessage());
            return ResponseEntity.status(500).body("Internal Server Error: " + e.getMessage());
        }
    }

    @PostMapping("/invalidate/{stockName}")
    public ResponseEntity<String> invalidate(@PathVariable("stockName") String stockName) {
        synchronized (cache) {
            cache.remove(stockName);
        }
        System.out.println("Cache invalidated for " + stockName);
        logger.info("Cache invalidated for {}", stockName);
        return ResponseEntity.ok("Cache invalidated for stock: " + stockName);
    }
}
