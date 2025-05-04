package com.example.frontend;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
@ConfigurationProperties(prefix = "order")
public class OrderLeaderSelector {

    private static final Logger logger = LoggerFactory.getLogger(OrderLeaderSelector.class);

    private String leaderUrl;
    private List<String> replicas;

    private final RestTemplate restTemplate = new RestTemplate();
    private final AtomicReference<String> currentLeader = new AtomicReference<>(null);

    public String getLeaderUrl() {
        return leaderUrl;
    }

    public void setLeaderUrl(String leaderUrl) {
        this.leaderUrl = leaderUrl;
    }

    public List<String> getReplicas() {
        return replicas;
    }

    public void setReplicas(List<String> replicas) {
        this.replicas = replicas;
    }

    public String getLeader() {
        String current = currentLeader.get();
        // ping before in order to verify if the current leader is alive
        if (current != null) {
            try {
                restTemplate.getForObject(current + "/orders/ping", Map.class);
                return current;
            } catch (Exception e) {
                logger.warn("current leader {} is unreachable, re-electing leader", current);
                currentLeader.set(null);
            }
        }
        // try to find new leader
        return findLeader();
    }

    public void resetLeader() {
        logger.warn("Resetting current leader...");
        currentLeader.set(null);
        findLeader();
    }

    public String findLeader() {
        if (replicas == null || replicas.isEmpty()) {
            logger.error("No replicas configured. Cannot select leader.");
            return null;
        }

        int maxId = -1;
        String selectedLeader = null;

        for (String url : replicas) {
            try {
                Map<?, ?> response = restTemplate.getForObject(url + "/orders/ping", Map.class);
                if (response != null && response.containsKey("replicaId")) {
                    int replicaId = (int) response.get("replicaId");
                    logger.info("Received ping from {} with replicaId {}", url, replicaId);
                    if (replicaId > maxId) {
                        maxId = replicaId;
                        selectedLeader = url;
                    }
                }
            } catch (Exception e) {
                logger.warn("Could not reach replica at {}: {}", url, e.getMessage());
            }
        }

        currentLeader.set(selectedLeader);
        if (selectedLeader != null) {
            logger.info("Leader selected: {}", selectedLeader);
        } else {
            logger.error("No available replicas responded, leader not selected!");
        }

        return selectedLeader;
    }
}

