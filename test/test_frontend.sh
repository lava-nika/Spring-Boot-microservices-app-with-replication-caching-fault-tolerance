#!/bin/bash

echo "sending order through frontend"
curl -s -X POST http://localhost:7070/orders \
  -H "Content-Type: application/json" \
  -d '{"name": "Stock1", "type": "buy", "quantity": 10}'

echo -e "\n getting stock via frontend, checking for cache hit or miss"
curl -s http://localhost:7070/stocks/Stock1

echo -e "\n Invalidating stock cache"
curl -X POST http://localhost:7070/stocks/invalidate/Stock1

echo -e "\n Retrieve that order"
curl http://localhost:7070/orders/1
