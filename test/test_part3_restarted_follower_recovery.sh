#!/bin/bash

echo "start only 2 order replicas, leave replica 3 down. enter to continue"
read

echo -e "\n sending order again via frontend, it will go to current leader (replica 2)"
curl -X POST http://localhost:7070/orders \
  -H "Content-Type: application/json" \
  -d '{"name": "Stock2", "type": "buy", "quantity": 50}'

echo -e "\n verify order is in replicas 1 and 2, missing in replica 3"
echo -e "\n replica 1:"
curl http://localhost:9091/orders/1
echo -e "\n replica 2:"
curl http://localhost:9092/orders/1
echo -e "\n replica 3:"
curl http://localhost:9093/orders/1

echo -e "\n start replica 3, it will recover order from current leader (replica 2), enter to continue"
read
curl http://localhost:9093/orders/1
