package no.ccat.service

import net.minidev.json.JSONArray
import no.ccat.utils.QueryParams
import org.junit.jupiter.api.Test
import testUtils.jsonPathParser
import testUtils.jsonValueParser
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
    fun `should return search with boosting on exact matches for full text searches`(){
        val expectedString: String = File("./src/test/resources/elasticsearch/full_text_query.json").readText(Charsets.UTF_8)
        val result = service.buildSearch(QueryParams(queryString = "dokument")).toString()

        val expectedScoreScript = jsonValueParser.parse(expectedString).read<JSONArray>("$..source").toString().split("||")
        val resultScoreScript = jsonValueParser.parse(result).read<JSONArray>("$..source").toString().split("||")

        expect(resultScoreScript).to_equal(expectedScoreScript)
        expect(jsonPathParser.parse(result)).json_to_have_entries_like(jsonPathParser.parse(expectedString))


    }

    @Test
    fun `should return search with boosting on exact match, and closer matches for prefLabel searches only`(){
        val expectedString: String = File("./src/test/resources/elasticsearch/prefLabel_only_query.json").readText(Charsets.UTF_8)
        val result = service.buildSearch(QueryParams(prefLabel = "dok")).toString()

        val expectedScoreScript = jsonValueParser.parse(expectedString).read<JSONArray>("$..source").toString().split("||")
        val resultScoreScript = jsonValueParser.parse(result).read<JSONArray>("$..source").toString().split("||")

        expect(resultScoreScript).to_equal(expectedScoreScript)
        expect(jsonPathParser.parse(result)).json_to_have_entries_like(jsonPathParser.parse(expectedString))
    }

}