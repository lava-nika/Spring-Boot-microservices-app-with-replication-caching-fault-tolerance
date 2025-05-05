#!/bin/bash

echo -e "\n test cache miss and fetching from catalog"
curl http://localhost:7070/stocks/Stock1

echo -e "\n repeat to show cache hit (no catalog call)"
curl http://localhost:7070/stocks/Stock1

echo -e "\n trigger cache invalidation, Stock1 removed from cache"
curl -X POST http://localhost:7070/stocks/invalidate/Stock1

# here we assume that cache size < number of queries
echo -e "\n to verify cache replacement"
echo -e "\n querying multiple stocks, cache replacement will be triggered"

for stock in Stock1 Stock2 Stock3 Stock4; do
  echo "querying $stock..."
  curl -s http://localhost:7070/stocks/$stock
done

