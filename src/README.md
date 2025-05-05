# Instructions to run 
## To start catalog service
```mvn spring-boot:run -pl src/catalog-service```

## To start frontend service
```mvn spring-boot:run -pl src/frontend-service```

## Order service replicas (Each in separate terminal)
### Replica 1 (Port 9091)
```mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=9091 --spring.profiles.active=replica1"```

### Replica 2 (Port 9092)
```mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=9092 --spring.profiles.active=replica2"```

### Replica 3 (Port 9093)
```mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=9093 --spring.profiles.active=replica3"```


