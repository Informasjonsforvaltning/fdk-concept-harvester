package no.ccat.utils.elasticsearch
import net.minidev.json.JSONArray
import no.ccat.utils.*
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import testUtils.jsonPathParser
import testUtils.jsonValueParser
import java.io.File
import testUtils.assertions.Expect as expect

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Tag("unit")

class SearchQueryBuilderTest {
    @Nested
    inner class ExactMatch {
        @Test
        fun `should return a bool query with terms query on raw prefLabelValues`() {
            val expectedString = File("./src/test/resources/elasticsearch/parts/exact_match_score.json").readText(Charsets.UTF_8)
            val result = buildExactMatchQuery("arkiv",LanguageProperties(key = "nb")).toString()

            expect(jsonPathParser.parse(result)).json_to_have_entries_like(jsonPathParser.parse(expectedString))
        }
    }
    @Nested
    inner class PrefLabelPrefixBoost{
        @Test
        fun `should return multi match query with differentiated boosting nb`(){
            val expectedString = File("./src/test/resources/elasticsearch/parts/mutli_match_prefix_prefLabel.json").readText(Charsets.UTF_8)
            val result = buildPrefixBoostQuery("arkiv",LanguageProperties(key = "nb")).toString()
            val expectedFieldsValue = listOf<String>("prefLabel.nn^2","prefLabel.nb^3","prefLabel.no^2","prefLabel.en^2")
            val resultFieldValue = jsonValueParser.parse(result).read<JSONArray>("$..multi_match.fields").toString()

            expectedFieldsValue.forEach {
                expect(resultFieldValue).to_contain(it)
            }
            expect(jsonPathParser.parse(result)).json_to_have_entries_like(jsonPathParser.parse(expectedString))
        }
        @Test
        fun `should return multi match query with differentiated boosting nn`(){
            val expectedString = File("./src/test/resources/elasticsearch/parts/mutli_match_prefix_prefLabel.json").readText(Charsets.UTF_8)
            val expectedFieldsValue = listOf<String>("prefLabel.nn^3","prefLabel.nb^2","prefLabel.no^2","prefLabel.en^2")
            val result = buildPrefixBoostQuery("arkiv",LanguageProperties(key = "nn")).toString()
            val resultFieldValue = jsonValueParser.parse(result).read<JSONArray>("$..multi_match.fields").toString()
            expectedFieldsValue.forEach {
                expect(resultFieldValue).to_contain(it)
            }
            expect(jsonPathParser.parse(result)).json_to_have_entries_like(jsonPathParser.parse(expectedString))
        }
        @Test
        fun `should return multi match query with differentiated boosting no`(){
            val expectedString = File("./src/test/resources/elasticsearch/parts/mutli_match_prefix_prefLabel.json").readText(Charsets.UTF_8)
            val result = buildPrefixBoostQuery("arkiv",LanguageProperties(key = "no")).toString()
            val expectedFieldsValue = listOf<String>("prefLabel.nn^2","prefLabel.nb^2","prefLabel.no^3","prefLabel.en^2")
            val resultFieldValue = jsonValueParser.parse(result).read<JSONArray>("$..multi_match.fields").toString()

            expectedFieldsValue.forEach {
                expect(resultFieldValue).to_contain(it)
            }
            expect(jsonPathParser.parse(result)).json_to_have_entries_like(jsonPathParser.parse(expectedString))
        }
        @Test
        fun `should return multi match query with differentiated boosting en`(){
            val expectedString = File("./src/test/resources/elasticsearch/parts/mutli_match_prefix_prefLabel.json").readText(Charsets.UTF_8)
            val expectedFieldsValue = listOf<String>("prefLabel.nn^2","prefLabel.nb^2","prefLabel.no^2","prefLabel.en^3")
            val result = buildPrefixBoostQuery("arkiv",LanguageProperties(key = "en")).toString()
            val resultFieldValue = jsonValueParser.parse(result).read<JSONArray>("$..multi_match.fields").toString()

            expectedFieldsValue.forEach {
                expect(resultFieldValue).to_contain(it)
            }
            expect(jsonPathParser.parse(result)).json_to_have_entries_like(jsonPathParser.parse(expectedString))
        }

    }
    @Nested
    inner class PrefLabelStringQuery{
        @Test
        fun `should return preflabel string query with differentiated boosting nb`(){
            val expectedString = File("./src/test/resources/elasticsearch/parts/query_string_prefLabel.json").readText(Charsets.UTF_8)
            val result = buildStringInPrefLabelQuery("arkiv",LanguageProperties(key = "nb")).toString()
            val expectedFieldsValue = listOf<String>("prefLabel.nn^1","prefLabel.nb^1.5","prefLabel.no^1","prefLabel.en^1")
            val expectedQueryValue = """["*arkiv*"]"""
            val resultFieldValue = jsonValueParser.parse(result).read<JSONArray>("$..query_string.fields").toString()
            val resultQueryValue = jsonValueParser.parse(result).read<JSONArray>("$..query_string.query").toString()

            expectedFieldsValue.forEach {
                expect(resultFieldValue).to_contain(it)
            }
            expect(resultQueryValue).to_equal(expectedQueryValue)
            expect(jsonPathParser.parse(result)).json_to_have_entries_like(jsonPathParser.parse(expectedString))
        }
        @Test
        fun `should return preflabel string query with differentiated boosting nn`(){
            val expectedString = File("./src/test/resources/elasticsearch/parts/query_string_prefLabel.json").readText(Charsets.UTF_8)
            val result = buildStringInPrefLabelQuery("arkiv",LanguageProperties(key = "nn")).toString()
            val expectedFieldsValue = listOf<String>("prefLabel.nn^1.5","prefLabel.nb^1","prefLabel.no^1","prefLabel.en^1")
            val resultFieldValue = jsonValueParser.parse(result).read<JSONArray>("$..query_string.fields").toString()
            expectedFieldsValue.forEach {
                expect(resultFieldValue).to_contain(it)
            }
            expect(jsonPathParser.parse(result)).json_to_have_entries_like(jsonPathParser.parse(expectedString))
        }
        @Test
        fun `should return  preflabel string query with differentiated boosting no`(){
            val expectedString = File("./src/test/resources/elasticsearch/parts/query_string_prefLabel.json").readText(Charsets.UTF_8)
            val result = buildStringInPrefLabelQuery("arkiv",LanguageProperties(key = "no")).toString()
            val expectedFieldsValue = listOf<String>("prefLabel.nn^1","prefLabel.nb^1","prefLabel.no^1.5","prefLabel.en^1")
            val resultFieldValue = jsonValueParser.parse(result).read<JSONArray>("$..query_string.fields").toString()
            expectedFieldsValue.forEach {
                expect(resultFieldValue).to_contain(it)
            }
            expect(jsonPathParser.parse(result)).json_to_have_entries_like(jsonPathParser.parse(expectedString))
        }
        @Test
        fun `should return preflabel string query with differentiated boosting en`(){
            val expectedString = File("./src/test/resources/elasticsearch/parts/query_string_prefLabel.json").readText(Charsets.UTF_8)
            val result = buildStringInPrefLabelQuery("arkiv",LanguageProperties(key = "en")).toString()
            val expectedFieldsValue = listOf<String>("prefLabel.nn^1","prefLabel.nb^1","prefLabel.no^1","prefLabel.en^1.5")
            val resultFieldValue = jsonValueParser.parse(result).read<JSONArray>("$..query_string.fields").toString()
            expectedFieldsValue.forEach {
                expect(resultFieldValue).to_contain(it)
            }
            expect(jsonPathParser.parse(result)).json_to_have_entries_like(jsonPathParser.parse(expectedString))
        }
    }
    @Nested
    inner class SimpleStringQuery{
        @Test
        fun `should return a simple string query with correct query string`() {
            val expectedString = File("./src/test/resources/elasticsearch/parts/simple_query_string_all.json").readText(Charsets.UTF_8)
            val result = buildSimpleStringQuery("arkiv").toString()
            val expectedQueryValue = """["arkiv arkiv*"]"""
            val resultQueryValue = jsonValueParser.parse(result).read<JSONArray>("$..simple_query_string.query").toString()

            expect(resultQueryValue).to_equal(expectedQueryValue)
            expect(jsonPathParser.parse(result)).json_to_have_entries_like(jsonPathParser.parse(expectedString))
        }
    }
    @Nested
    inner class orgPathQuery
    {
        @Test
        fun `should return a match orgPath query`(){
            val expectedMatch = File("./src/test/resources/elasticsearch/org_path_macth_query.json").readText(Charsets.UTF_8)
            val result = buildOrgPathQuery(QueryParams(orgPath = "ANNET/1234598")).toString()

            expect(result).json_to_equal(expectedMatch)
        }

        @Test
        fun `should return a bool exists orgPath query`(){
            val expectedMatch = File("./src/test/resources/elasticsearch/org_path_macth_missing_query.json").readText(Charsets.UTF_8)
            val result = buildOrgPathQuery(QueryParams(orgPath = "MISSING")).toString()

            expect(result).json_to_equal(expectedMatch)
        }

    }

    @Nested
    inner class buildSearchUrisQueryTest
    {
        val expectedQuery = File("./src/test/resources/elasticsearch/uris_match_search_query.json").readText(Charsets.UTF_8)

        val uris = setOf("http://localhost/concept/1", "http://localhost/concept/2")

        @Test
        fun `should return a builder containing 2 concept URIs`(){
            val result = buildUrisQuery(uris)
            expect(result.toString()).json_to_equal(expectedQuery)
        }
    }
}