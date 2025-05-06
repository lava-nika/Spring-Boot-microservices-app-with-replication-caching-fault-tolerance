package com.example.frontend;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// manages leader selection, pings replicas and chooses the one with highest replicaID as leader
@Component
@ConfigurationProperties(prefix = "order")
public class OrderLeaderSelector {

    private static final Logger logger = LoggerFactory.getLogger(OrderLeaderSelector.class);

    // static leader url
    private String leaderUrl;

    // list of replica URLs (like http://localhost:9091)
    private List<String> replicas;

    // REST client for pinging replicas
    private final RestTemplate restTemplate = new RestTemplate();

    // tracks current leader in a thread-safe manner
    private final AtomicReference<String> currentLeader = new AtomicReference<>(null);

    // getter and setter for leader url and replicas
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

    // returns current leader, sends ping to verify if it is alive
    // if it is unreachable, starts re-election using findLeader() function
    public String getLeader() {
        String current = currentLeader.get();
        // ping current leader to confirm if it is alive
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

    // forces current leader to be reset and triggers re-election
    public void resetLeader() {
        logger.warn("Resetting current leader");
        currentLeader.set(null);
        findLeader();
    }

    // elects new leader by pinging all replicas and selecting the one with highest replicaID
    // this function assumes that current leader is the replica with the highest ID
    public String findLeader() {
        if (replicas == null || replicas.isEmpty()) {
            logger.error("No replicas configured, cannot select leader!");
            return null;
        }

        int maxId = -1;
        String selectedLeader = null;

        for (String url : replicas) {
            try {
                // send a ping request to each replica
                Map<?, ?> response = restTemplate.getForObject(url + "/orders/ping", Map.class);
                if (response != null && response.containsKey("replicaId")) {
                    int replicaId = (int) response.get("replicaId");
                    logger.info("Received ping from {} with replicaId {}", url, replicaId);
                    //  select the replica with the highest ID
                    if (replicaId > maxId) {
                        maxId = replicaId;
                        selectedLeader = url;
                    }
                }
            } catch (Exception e) {
                logger.warn("Could not reach replica at {}: {}", url, e.getMessage());
            }
        }
        // update current leader
        currentLeader.set(selectedLeader);
        if (selectedLeader != null) {
            logger.info("Leader selected: {}", selectedLeader);
        } else {
            logger.error("No available replicas responded, leader not selected!");
        }

        return selectedLeader;
    }
}

