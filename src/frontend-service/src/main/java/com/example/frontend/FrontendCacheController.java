package com.example.frontend;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

//import javax.annotation.PostConstruct;
import jakarta.annotation.PostConstruct;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/stocks")
public class FrontendCacheController {

    @Value("${cache.size:10}")
    private int cacheSize;

    private Map<String, String> cache;

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
        synchronized (cache) {
            if (cache.containsKey(stockName)) {
                System.out.println("CACHE HIT for " + stockName);
                return ResponseEntity.ok(cache.get(stockName));
            }
        }

        System.out.println("CACHE MISS for " + stockName);

        try {
            String catalogUrl = System.getenv().getOrDefault("CATALOG_URL", "http://localhost:8081");
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
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(status).body(response);
            }
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Internal Server Error: " + e.getMessage());
        }
    }

    @PostMapping("/invalidate/{stockName}")
    public ResponseEntity<String> invalidate(@PathVariable("stockName") String stockName) {
        synchronized (cache) {
            cache.remove(stockName);
        }
        System.out.println("Cache invalidated for " + stockName);
        return ResponseEntity.ok("Cache invalidated for stock: " + stockName);
    }
}
