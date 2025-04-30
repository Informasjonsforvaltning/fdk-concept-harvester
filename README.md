# FDK Concept Harvester

This application provides an API for concepts harvested from different sources.

For a broader understanding of the system’s context, refer to
the [architecture documentation](https://github.com/Informasjonsforvaltning/architecture-documentation) wiki. For more
specific context on this application, see the **Harvesting** subsystem section.

The harvest process is triggered by messages from RabbitMQ with the routing key `concept.*.HarvestTrigger`, a message
will call the method `initiateHarvest` in the class `HarvesterActivity`. The actual harvest will start when
`activitySemaphore` has an available permit, when there are no available permits all messages will be queued by the
semaphore.

The body of the trigger message has 3 relevant parameters:

- `dataSourceId` - Triggers the harvest of a specific source from fdk-harvest-admin
- `publisherId` - Triggers the harvest of all sources for the specified organization number.
- `forceUpdate` - Indicates that the harvest should be performed, even when no changes are detected in the source

A triggered harvest will download all relevant sources from fdk-harvest-admin, download everything from the source and
try to read it as a RDF graph via a jena Model, TBX-sources will be transformed to RDF. If the source is successfully
parsed as a jena Model it will be compared to the last harvest of the same source. The harvest process will continue if
the source is not isomorphic to the last harvest or `forceUpdate` is true.

The actual harvest process will first find concepts, resources with the type `skos:Concept`, and collections, resources
with the type `skos:Collection`, blank node concepts and collections will be ignored. A collection will also be
generated from data about the source from fdk-harvest-admin, any concepts in the source that is not part of any
collection, indicated by the predicate `skos:member`, will be added to this collection.
When all collections and concepts have been found a recursive function will create a graph with every contained triple
for all collections and concepts.

The process will save metadata for both concepts and collections:

- `uri` - The IRI for the resource, is used as the database id
- `fdkId` - The UUID used for the resource used in the context of FDK, is a generated hash of the uri if nothing else is
  set.
- `isPartOf` - Only relevant for concepts, is the uri of the collection it belongs to.
- `removed` - Only relevant for concepts, is set to true if the concept has been removed from the source.
- `issued` - The timestamp of the first time the resource was harvested
- `modified` - The timestamp of the last time a harvest of the resource found changes in the resource graph

All blank nodes will be [skolemized](https://www.w3.org/wiki/BnodeSkolemization) in the resource graphs.

When all sources from the trigger has been processed a new rabbit message will be published with the routing key
`concepts.harvested`, the message body will be a list of harvest reports, one report for each source from
fdk-harvest-admin.

When the rabbit message has been published the semaphore permit is released and a new harvest trigger can be processed.

## Getting Started

These instructions will give you a copy of the project up and running on your local machine for development and testing
purposes.

### Prerequisites

Ensure you have the following installed:

- Java 17
- Maven
- Docker

### Running locally

#### Clone the repository

```sh
git clone https://github.com/Informasjonsforvaltning/fdk-concept-harvester.git
cd fdk-concept-harvester
```

Start MongoDB, RabbitMQ and the application (either through your IDE using the dev profile, or via CLI):

```sh
docker compose up -d
mvn spring-boot:run -Dspring-boot.run.profiles=develop
```

### API Documentation (OpenAPI)

The API documentation is available at ```resources/specification```.

### Running tests

```sh
mvn verify
```
