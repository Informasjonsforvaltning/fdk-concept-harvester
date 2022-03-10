package no.fdk.fdk_concept_harvester.adapter

import no.fdk.fdk_concept_harvester.harvester.HarvestException
import no.fdk.fdk_concept_harvester.model.HarvestDataSource
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import java.io.BufferedReader
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.Charset

private val LOGGER = LoggerFactory.getLogger(ConceptsAdapter::class.java)

@Service
class ConceptsAdapter {

    fun getConcepts(source: HarvestDataSource): String? {
        val connection = URL(source.url).openConnection() as HttpURLConnection
        try {
            connection.setRequestProperty(HttpHeaders.ACCEPT, source.acceptHeaderValue)

            return if (connection.responseCode != HttpStatus.OK.value()) {
                LOGGER.error("${source.url} responded with ${connection.responseCode}, harvest will be aborted", HarvestException(source.url ?: "undefined"))
                null
            } else {
                val charset = if(connection.contentEncoding != null)
                    Charset.forName(connection.contentEncoding) else Charsets.UTF_8
                connection
                    .inputStream
                    .bufferedReader(charset)
                    .use(BufferedReader::readText)
            }

        } catch (ex: Exception) {
            LOGGER.error("Error when harvesting from ${source.url}", ex)
            return null
        } finally {
            connection.disconnect()
        }
    }

}
