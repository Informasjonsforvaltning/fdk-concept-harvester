package no.fdk.fdk_concept_harvester.controller

import no.fdk.fdk_concept_harvester.rdf.jenaTypeFromAcceptHeader
import no.fdk.fdk_concept_harvester.service.ConceptService
import org.apache.jena.riot.Lang
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.*

private val LOGGER = LoggerFactory.getLogger(ConceptsController::class.java)

@Controller
@CrossOrigin
@RequestMapping(
    value = ["/concepts"],
    produces = ["text/turtle", "text/n3", "application/rdf+json", "application/ld+json", "application/rdf+xml",
        "application/n-triples", "application/n-quads", "application/trig", "application/trix"]
)
open class ConceptsController(private val conceptService: ConceptService) {

    @GetMapping("/{id}")
    fun getConceptById(
        @RequestHeader(HttpHeaders.ACCEPT) accept: String?,
        @PathVariable id: String,
        @RequestParam(value = "catalogrecords", required = false) catalogrecords: Boolean = false
    ): ResponseEntity<String> {
        LOGGER.info("get concept with id $id")
        val returnType = jenaTypeFromAcceptHeader(accept)

        return if (returnType == Lang.RDFNULL) ResponseEntity(HttpStatus.NOT_ACCEPTABLE)
        else {
            conceptService.getConceptById(id, returnType ?: Lang.TURTLE, catalogrecords)
                ?.let { ResponseEntity(it, HttpStatus.OK) }
                ?: ResponseEntity(HttpStatus.NOT_FOUND)
        }
    }

}
