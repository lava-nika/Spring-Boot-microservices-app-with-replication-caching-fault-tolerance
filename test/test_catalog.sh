#!/bin/bash

echo "stock update (buy or sell)"
curl -s -X POST http://localhost:8081/stocks/trade \
  -H "Content-Type: application/json" \
  -d '{"name": "Stock1", "type": "buy", "quantity": 5}'

echo -e "\n test catalog stock retrieval"
curl -s http://localhost:8081/stocks/Stock1
