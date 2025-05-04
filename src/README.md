# Instructions to run 
## To start catalog service
```mvn spring-boot:run -pl src/catalog-service```

## To start frontend service
```mvn spring-boot:run -pl src/frontend-service```

## Order service replicas (Each in separate terminal)
### Replica 1 (Port 9091)
```mvn spring-boot:run -pl src/order-service -Dspring-boot.run.profiles=replica1```

### Replica 2 (Port 9092)
```mvn spring-boot:run -pl src/order-service -Dspring-boot.run.profiles=replica2```

### Replica 3 (Port 9093)
```mvn spring-boot:run -pl src/order-service -Dspring-boot.run.profiles=replica3```


