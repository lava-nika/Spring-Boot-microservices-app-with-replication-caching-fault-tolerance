#!/bin/bash

echo "bring down leader replica (at 9093) now (ctrl+c). enter to continue"
read

echo -e "\n sending order again via frontend, it will switch to the next leader"
curl -s -X POST http://localhost:7070/orders \
  -H "Content-Type: application/json" \
  -d '{"name": "Stock2", "type": "sell", "quantity": 3}'

echo -e "\n frontend picks new leader and continues working"

