# Concept-cat: Concept Catalog Service

## Overview

Module for harvesting and querying concepts.

## Environment Variables

    - RABBIT_USERNAME
    - RABBIT_PASSWORD

## Data types

### ApiDocument

##Development:
1. Start necessary containers `docker-compose up`
2. Start application with commandline argument `-Dspring.profiles.active=develop` from [CcatApiApplication.java](src/main/java/no/ccat/CcatApiApplication.java)
3. Default port is 8105, (see `server` entry in [application.yml](src/main/resources/application.yml) )


## Debugging Elasticsearch

See all stored documents
```
GET 
localhost:9200/_search
Body:
{
    "from" : 0, "size" : <number of documents>,
    "query" : {
        "match_all" : {} 
    }
}
```




