package no.fdk.fdk_concept_harvester.adapter

import no.fdk.fdk_concept_harvester.harvester.HarvestException
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import java.io.BufferedReader
import java.net.HttpURLConnection
import java.net.URI
import java.nio.charset.Charset

@Service
class ConceptsAdapter {

    fun getConcepts(url: String, acceptHeader: String): String {
        val connection = URI(url).toURL().openConnection() as HttpURLConnection
        try {
            connection.setRequestProperty(HttpHeaders.ACCEPT, acceptHeader)

            return if (connection.responseCode != HttpStatus.OK.value()) {
                throw HarvestException("${url} responded with ${connection.responseCode}, harvest will be aborted")
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
