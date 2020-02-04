package no.ccat.service

import no.ccat.utils.QueryParams
import org.junit.jupiter.api.Test
import java.io.File
import testUtils.assertions.Expect as expect
class EsSearchServiceTest {
    private val service = EsSearchService()

    @Test
    fun `should return empty match all request`() {
        val expected = "{\n" +
                              "  \"match_all\" : {\n" +
                              "    \"boost\" : 1.0\n" +
                              "  }\n" +
                            "}"
        val result = service.buildSearch(QueryParams()).toString()

        expect(result).to_equal(expected)
    }

    @Test
    fun `should return search with boosting on title matches`(){
        val expectedString: String = File("./src/test/resources/elasticsearch/stringQuerySearch.json").readText(Charsets.UTF_8)

        val result = service.buildSearch(QueryParams(queryString = "dokument")).toString()

        expect(result).json_to_equal(expectedString)
    }

    @Test
    fun `should return search with prefLabel matcher`(){
    }
}