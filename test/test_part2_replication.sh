#!/bin/bash

echo -e "sending order via frontend"
curl -X POST http://localhost:7070/orders \
  -H "Content-Type: application/json" \
  -d '{"name": "Stock2", "type": "buy", "quantity": 10}'

echo -e "\n verify if order exists for all replicas"
curl http://localhost:9091/orders/1
curl http://localhost:9092/orders/1
curl http://localhost:9093/orders/1
