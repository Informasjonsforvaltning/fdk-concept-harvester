package no.fdk.fdk_concept_harvester.tbx

import no.fdk.fdk_concept_harvester.rdf.parseRDFResponse
import no.fdk.fdk_concept_harvester.utils.TestOrganizationsAdapter
import no.fdk.fdk_concept_harvester.utils.TestResponseReader
import org.apache.jena.riot.Lang
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

@Tag("unit")
class TbxTest {

    private val responseReader = TestResponseReader()

    @Test
    fun tbxParse() {
            val tbxModel = parseTBXResponse(responseReader.readFile("tbx.xml"), "dummy", TestOrganizationsAdapter())
            val ttlModel = parseRDFResponse(responseReader.readFile("tbx.ttl"), Lang.TTL, "dummy")
            assertTrue(ttlModel?.isIsomorphicWith(tbxModel) ?: false)
    }

    @Test
    fun tbxParseWithoutPublisher() {
        val tbxModel = parseTBXResponse(responseReader.readFile("tbx_no_publisher.xml"), "dummy", TestOrganizationsAdapter())
        val ttlModel = parseRDFResponse(responseReader.readFile("tbx_no_publisher.ttl"), Lang.TTL, "dummy")
        assertTrue(ttlModel?.isIsomorphicWith(tbxModel) ?: false)
    }
}