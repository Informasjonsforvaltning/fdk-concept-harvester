# fdk-concept-harvester

## Requirements
- maven
- java 15
- docker
- docker-compose

## Run tests
```
mvn verify
```

## Run locally
```
docker-compose up -d
mvn spring-boot:run -Dspring-boot.run.profiles=develop
```

Then in another terminal e.g.
```
% curl http://localhost:8080/collections
```

## Datastore
To inspect the MongoDB datastore, open a terminal and run:
```
docker-compose exec mongodb mongo
use admin
db.auth("admin","admin")
use conceptHarvester
db.collectionMeta.find()
db.conceptMeta.find()
db.turtle.find()
```
