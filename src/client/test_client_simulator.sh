#!/bin/bash

for i in {1..10}; do
    stock="Stock$((RANDOM % 6 + 1))"
    quantity=$((RANDOM % 5 + 1))
    if (( RANDOM % 2 == 0 )); then
      type="buy"
    else
      type="sell"
    fi

    echo "sending order: $type $quantity of $stock"
    curl -s -X POST http://localhost:7070/orders \
        -H "Content-Type: application/json" \
        -d "{\"name\":\"$stock\",\"type\":\"$type\",\"quantity\":$quantity}" \
        > /dev/null
    sleep 1
done
