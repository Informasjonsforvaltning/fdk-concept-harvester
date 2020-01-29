package no.ccat.service

import lombok.RequiredArgsConstructor
import no.ccat.common.model.ConceptDenormalized
import no.ccat.dto.HarvestDataSource
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.io.IOException
import java.io.Reader
import java.io.StringReader
import java.net.URL
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*
import java.util.function.Consumer
import javax.annotation.PostConstruct

private val logger = LoggerFactory.getLogger(ConceptHarvester::class.java)

/*
    Fetch concepts and insert or update them in the search index.
 */
@Service
@RequiredArgsConstructor
class ConceptHarvester (
    private val conceptDenormalizedRepository: ConceptDenormalizedRepository,
    private val rdfToModelTransformer: RDFToModelTransformer,
    private val harvestAdminClient: HarvestAdminClient
) {

    private var isRunningForDeveloperLocally: Boolean = false

    @PostConstruct
    private fun harvestOnce() {
        logger.info("Harvest of Concepts start")
        harvest(harvestAdminClient.dataSources)
        logger.info("Harvest of Concepts complete")
    }

    fun harvest(datasources: List<HarvestDataSource>) {
        datasources.forEach(this::harvestFromSingleURLSource)
    }

    fun harvestFromSingleURLSource(dataSource: HarvestDataSource) {
        val reader: Reader
        var theEntireDocument: String? = null

        theEntireDocument = if (isRunningForDeveloperLocally) {
            logger.info("Harvester isRunningForDeveloperLocally==true")
            readFileFully("c:\\tmp\\localConceptsFile.txt")
        } else {
            readURLFully(dataSource.url, dataSource.acceptHeaderValue)
        }

        if (theEntireDocument == null) return
        else {
            reader = StringReader(theEntireDocument)
            val concepts = rdfToModelTransformer.getConceptsFromStream(reader)
            logger.info("Harvested {} concepts from publisher {} at Uri {} ", concepts.size, dataSource.publisherId, dataSource.url)
            concepts.forEach(Consumer { s: ConceptDenormalized -> conceptDenormalizedRepository.save(s) })
        }
    }

    private fun readFileFully(fileURI: String): String? {
        try {
            return String(Files.readAllBytes(Paths.get(fileURI)))
        } catch (ie: IOException) {
            logger.warn("File load failed for File URI $fileURI")
        }
        return null
    }

    private fun readURLFully(harvestSourceUri: String?, acceptHeader: String?): String {
        try {
            val url = URL(harvestSourceUri)
            logger.info("Start harvest from url: {}", url)
            val urlConnection = url.openConnection()
            urlConnection.setRequestProperty("Accept", acceptHeader)
            val inputStream = urlConnection.getInputStream()
            val s = Scanner(inputStream).useDelimiter("\\A")
            return if (s.hasNext()) s.next() else ""
        } catch (e: IOException) {
            logger.warn("Downloading concepts from url failed: {} Exception {}", harvestSourceUri, e.toString())
            logger.trace("Got exception when trying to harvest from url {} {}", harvestSourceUri, e.stackTrace)
        }
        return ""
    }
}