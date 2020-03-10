package no.ccat.service

import net.minidev.json.JSONArray
import no.ccat.utils.QueryParams
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import testUtils.jsonPathParser
import testUtils.jsonValueParser
import testUtils.paramsWithIdentifiers
import java.io.File
import testUtils.assertions.Expect as expect


@Tag("unit")
class EsSearchServiceTest {
    private val service = EsSearchService()

    @Test
    fun `should return empty match all query`() {
        val expected = "{\n" +
                              "  \"match_all\" : {\n" +
                              "    \"boost\" : 1.0\n" +
                              "  }\n" +
                            "}"
        val result = service.buildSearch(QueryParams()).toString()
        expect(result).to_equal(expected)
    }
    @Test
    fun `should return query with boosting on exact matches for full text searches`(){
        val expectedString: String = File("./src/test/resources/elasticsearch/full_text_query.json").readText(Charsets.UTF_8)
        val result = service.buildSearch(QueryParams(queryString = "dokument")).toString()

        val expectedScoreScript = jsonValueParser.parse(expectedString).read<JSONArray>("$..filter.script.script.source")
                .toString()
                .split("||")
        val resultScoreScript = jsonValueParser.parse(result).read<JSONArray>("$..filter.script.script.source").toString().split("||")

        expect(resultScoreScript).to_equal(expectedScoreScript)
        expect(jsonPathParser.parse(result)).json_to_have_entries_like(jsonPathParser.parse(expectedString))
    }
    @Test
    fun `should return query with boosting on exact match, and closer matches for prefLabel searches only`(){
        val expectedString: String = File("./src/test/resources/elasticsearch/prefLabel_only_query.json").readText(Charsets.UTF_8)
        val result = service.buildSearch(QueryParams(prefLabel = "dok")).toString()

        expect(jsonPathParser.parse(result)).json_to_have_entries_like(jsonPathParser.parse(expectedString))
    }
    @Test
    fun `should return full text query with orgPath match`() {
        val expectedString: String = File("./src/test/resources/elasticsearch/full_text_org_path_query.json").readText(Charsets.UTF_8)
        val result = service.buildSearch(QueryParams(queryString = "arkiv", orgPath = "STAT/87654321/12345678")).toString()

        expect(jsonPathParser.parse(result)).json_to_have_entries_like(jsonPathParser.parse(expectedString))

    }
    @Test
    fun `should return prefLabel query with orgPath match`() {
        val expectedString: String = File("./src/test/resources/elasticsearch/prefLabel_org_path_query.json").readText(Charsets.UTF_8)
        val result = service.buildSearch(QueryParams(prefLabel = "arkiv", orgPath = "STAT/87654321/12345678")).toString()

        expect(jsonPathParser.parse(result)).json_to_have_entries_like(jsonPathParser.parse(expectedString))

    }
    @Test
    fun `should return "MISSING" orgPath match`() {
        val expectedString: String = File("./src/test/resources/elasticsearch/org_path_macth_missing_query.json").readText(Charsets.UTF_8)
        val result = service.buildSearch(QueryParams(orgPath = "MISSING")).toString()

         expect(jsonPathParser.parse(result)).json_to_have_entries_like(jsonPathParser.parse(expectedString))

    }
    @Test
    fun `should return prefLabel search with "MISSING" orgPath`() {
        val expectedString: String = File("./src/test/resources/elasticsearch/prefLabel_org_path_missing_query.json").readText(Charsets.UTF_8)
        val result = service.buildSearch(QueryParams(queryString = "some",orgPath = "MISSING")).toString()

        expect(jsonPathParser.parse(result)).json_to_have_entries_like(jsonPathParser.parse(expectedString))

    }
    @Test
    fun `should return orgPath match`() {
        val expectedString: String = File("./src/test/resources/elasticsearch/prefLabel_org_path_query.json").readText(Charsets.UTF_8)
        val result = service.buildSearch(QueryParams(prefLabel = "arkiv", orgPath = "STAT/87654321/12345678")).toString()
        expect(jsonPathParser.parse(result)).json_to_have_entries_like(jsonPathParser.parse(expectedString))

    }
    @Test
    fun `should return uris query`() {
        val expectedString: String = File("./src/test/resources/elasticsearch/uris_search_query.json").readText(Charsets.UTF_8)
        val result = service.buildSearch(QueryParams(uris = setOf("http://localhost/concept/1", "http://localhost/concept/2"))).toString()

        expect(jsonPathParser.parse(result)).json_to_have_entries_like(jsonPathParser.parse(expectedString))
    }
    @Test
    fun `should return identifiers query`() {
        val expectedString: String = File("./src/test/resources/elasticsearch/identifiers_search_query.json").readText(Charsets.UTF_8)
        val result = service.buildSearch(paramsWithIdentifiers()).toString()

        expect(jsonPathParser.parse(result)).json_to_have_entries_like(jsonPathParser.parse(expectedString))
    }
}