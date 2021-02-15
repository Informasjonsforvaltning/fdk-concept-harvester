package no.fdk.fdk_concept_harvester.adapter

import no.fdk.fdk_concept_harvester.configuration.FusekiProperties
import no.fdk.fdk_concept_harvester.rdf.createRDFResponse
import org.apache.jena.rdf.model.Model
import org.apache.jena.riot.Lang
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

private val LOGGER = LoggerFactory.getLogger(FusekiAdapter::class.java)

@Service
class FusekiAdapter(private val fusekiProperties: FusekiProperties) {

    fun storeUnionModel(model: Model) {
        LOGGER.debug("Updating fuseki graph")
        with(URL(fusekiProperties.unionGraphUri).openConnection() as HttpURLConnection) {
            try {

                setRequestProperty(HttpHeaders.CONTENT_TYPE, "application/rdf+xml")
                requestMethod = "PUT"
                doOutput = true

                OutputStreamWriter(outputStream).use {
                    it.write(model.createRDFResponse(Lang.RDFXML))
                    it.flush()
                }

                if (HttpStatus.valueOf(responseCode).is2xxSuccessful) {
                    LOGGER.debug("Save to fuseki completed, status: $responseCode")
                } else {
                    LOGGER.error("Save to fuseki failed, status: $responseCode")
                }
            } catch (ex: Exception) {
                LOGGER.error("Error when saving to fuseki", ex)
            } finally {
                disconnect()
            }
        }
    }

}
