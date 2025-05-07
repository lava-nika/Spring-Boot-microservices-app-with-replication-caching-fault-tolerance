#!/bin/bash

# here cache size is 3
echo "testing cache replacement in action"
stock_names=("Stock1" "Stock2" "Stock3" "Stock4" "Stock5")

for stock in "${stock_names[@]}"
do
    echo "querying $stock"
    curl -s http://localhost:7070/stocks/$stock > /dev/null
    sleep 1
done


echo "frontend.log will have all the logs"

