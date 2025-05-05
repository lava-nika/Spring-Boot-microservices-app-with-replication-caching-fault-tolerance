# Test Scripts

This directory contains the shell scripts to test the working of all the microservices.

### Note:
All 3 microservices here are running locally (frontend: `7070`, catalog: `8080`, order replicas: `9091`, `9092`, `9093`)

To run all scripts from test directory
1) Make them executable ```chmod +x *.sh```
2) To run each test (do for each .sh)
```./test_frontend.sh```

List of .sh (test) files: 

1) `test_catalog.sh` : to test catalog service functionality, and verify price and quantity updates
2) `test_frontend.sh` : to test cache hit or miss, invalidate cache, retrieve order from frontend
3) `test_part1_caching.sh` : to send stock query requests and check all cache behavior + replacement
4) `test_part2_replication.sh` : to send order via frontend and test if order exists for all replicas
5) `test_part3_leader_failure.sh` : to test leader replica failure and verify recovery
6) `test_part3_restarted_follower_recovery.sh` : bring down a replica and check if it syncs with current leader after bringing it up 