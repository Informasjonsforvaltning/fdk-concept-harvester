package no.ccat.service

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
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

    @PostConstruct
    private fun harvestOnce() {
        logger.info("Harvest of Concepts start")
        harvest(harvestAdminClient.dataSources)
    }

    fun harvest(dataSources: List<HarvestDataSource>) {
        GlobalScope.launch { dataSources.forEach{ harvestFromSingleURLSource(it) } }
    }

    private fun harvestFromSingleURLSource(dataSource: HarvestDataSource) {
        val concepts = StringReader(readURLFully(dataSource.url, dataSource.acceptHeaderValue))
            .let { rdfToModelTransformer.getConceptsFromStream(it) }

        logger.info("Harvested {} concepts from publisher {} at Uri {} ", concepts.size, dataSource.publisherId, dataSource.url)

        concepts.forEach(Consumer { s: ConceptDenormalized -> conceptDenormalizedRepository.save(s) })
    }

    private fun readURLFully(harvestSourceUri: String?, acceptHeader: String?): String {
        try {
            logger.info("Start harvest from url: {}", harvestSourceUri)
            val urlConnection = URL(harvestSourceUri).openConnection()
            urlConnection.setRequestProperty("Accept", acceptHeader)

            val s = Scanner(urlConnection.getInputStream()).useDelimiter("\\A")

            return if (s.hasNext()) s.next() else ""
        } catch (e: IOException) {
            logger.warn("Downloading concepts from url failed: {} Exception {}", harvestSourceUri, e.toString())
            logger.trace("Got exception when trying to harvest from url {} {}", harvestSourceUri, e.stackTrace)
        }
        return ""
    }
}