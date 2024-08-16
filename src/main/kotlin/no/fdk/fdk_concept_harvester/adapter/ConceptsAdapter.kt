package no.fdk.fdk_concept_harvester.adapter

import no.fdk.fdk_concept_harvester.harvester.HarvestException
import no.fdk.fdk_concept_harvester.model.HarvestDataSource
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import java.io.BufferedReader
import java.net.HttpURLConnection
import java.net.URI
import java.nio.charset.Charset

@Service
class ConceptsAdapter {

    fun getConcepts(source: HarvestDataSource): String {
        val connection = URI(source.url).toURL().openConnection() as HttpURLConnection
        try {
            connection.setRequestProperty(HttpHeaders.ACCEPT, source.acceptHeaderValue)
            source.authHeader?.run { connection.setRequestProperty(name, value) }

            return if (connection.responseCode != HttpStatus.OK.value()) {
                throw HarvestException("${source.url} responded with ${connection.responseCode}, harvest will be aborted")
            } else {
                val charset = if(connection.contentEncoding != null)
                    Charset.forName(connection.contentEncoding) else Charsets.UTF_8
                connection
                    .inputStream
                    .bufferedReader(charset)
                    .use(BufferedReader::readText)
            }

        } finally {
            connection.disconnect()
        }
    }

}
