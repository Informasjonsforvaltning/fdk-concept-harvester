package no.ccat.contract

import net.minidev.json.JSONArray
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import testUtils.ApiTestContainer
import testUtils.SortResponse
import testUtils.assertions.apiGet
import testUtils.jsonPathParser
import testUtils.jsonValueParser
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import testUtils.assertions.Expect as expect

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Tag("contract")

class ConceptContractTest : ApiTestContainer() {
    val conceptsPath = "$.._embedded.concepts"
    val prefLabelPath = "$conceptsPath.*.prefLabel"
    val orgPath = "publisher.orgPath"
    val testString = "dokument"


    @Nested
    /**
     * id operationId: searchUsingGET
     */
    inner class SearchUsingText {

        inner class Sort {
            // TODO
        }

        inner class Aggregations {
            //TODO
        }

        @TestInstance(TestInstance.Lifecycle.PER_CLASS)
        @Nested
        inner class FullTextSearch {


            @Test
            fun `expect full text search to return list of concepts sorted on exact matches in preferredLanguage nb`() {

                val result = apiGet("/concepts?q=$testString&size=100")
                val pathParser = jsonPathParser.parse(result)
                val valueParser = jsonValueParser.parse(result)
                val sortResult = SortResponse(sortWord = testString,
                        pathParser = pathParser)

                val responsePaths = sortResult.getPathsForField(jsonPath = prefLabelPath)


                for (i in responsePaths.indices) {
                    val currentValue = valueParser.read<JSONArray>(responsePaths[i])
                    val currentId = valueParser.read<JSONArray>("$..concepts[$i].id").toString()
                    sortResult.expectIsLessRelevant(currentValue.toString(), i, currentId)
                }
            }

            @Test
            fun `expect full text search with trailing plus to return list of concepts sorted on exact matches in preferredLanguage nb`() {

                val result = apiGet("/concepts?q=$testString++&size=100")
                val pathParser = jsonPathParser.parse(result)
                val valueParser = jsonValueParser.parse(result)
                val sortResult = SortResponse(sortWord = testString,
                        pathParser = pathParser)

                val responsePaths = sortResult.getPathsForField(jsonPath = prefLabelPath)


                for (i in responsePaths.indices) {
                    val currentValue = valueParser.read<JSONArray>(responsePaths[i])
                    val currentId = valueParser.read<JSONArray>("$..concepts[$i].id").toString()
                    sortResult.expectIsLessRelevant(currentValue.toString(), i, currentId)
                }
            }

            @Test
            fun `expect full text search with orgPath to return list of concepts from corresponding publisher only`() {
                val result = apiGet("/concepts?q=$testString&size=100&orgPath=STAT/87654321/12345678")

                val expOrgPath = "STAT/87654321/12345678".split("/")
                val resultOrgPath = jsonValueParser.parse(result).read<List<String>>("\$.._embedded.concepts.*.publisher.orgPath")

                resultOrgPath.forEach { expect(it).to_be_organisation(expOrgPath) }
            }

            @Test
            fun `expect full text search with orgPath and trailing plus chars to return list of concepts from corresponding publisher only`() {
                val result = apiGet("/concepts?q=$testString++&size=100&orgPath=STAT/87654321/12345678")

                val expOrgPath = "STAT/87654321/12345678".split("/")
                val resultOrgPath = jsonValueParser.parse(result).read<List<String>>("\$.._embedded.concepts.*.publisher.orgPath")

                resultOrgPath.forEach { expect(it).to_be_organisation(expOrgPath) }
            }
        }
    }

    @Nested
    inner class PrefLabelOnlySearch {
        @Test
        fun `expect prefLabel search to return list of concepts sorted on exact matches in preferredLanguage `() {
            val result = apiGet("/concepts?prefLabel=$testString&size=100")
            val pathParser = jsonPathParser.parse(result)
            val valueParser = jsonValueParser.parse(result)
            val sortResult = SortResponse(sortWord = testString,
                    pathParser = pathParser)

            val responsePaths = sortResult.getPathsForField(jsonPath = prefLabelPath)


            for (i in responsePaths.indices) {
                val currentValue = valueParser.read<JSONArray>(responsePaths[i]).toString()
                val currentId = valueParser.read<JSONArray>("$..concepts[$i].id").toString()
                sortResult.expectIsLessRelevant(currentValue, i, currentId)
                expect(currentValue).to_contain(testString)
            }

        }

        @Test
        fun `expect prefLabel search with trailing plus to return list of concepts sorted on exact matches in preferredLanguage `() {
            val result = apiGet("/concepts?prefLabel=$testString&size=100")
            val pathParser = jsonPathParser.parse(result)
            val valueParser = jsonValueParser.parse(result)
            val sortResult = SortResponse(sortWord = testString,
                    pathParser = pathParser)

            val responsePaths = sortResult.getPathsForField(jsonPath = prefLabelPath)


            for (i in responsePaths.indices) {
                val currentValue = valueParser.read<JSONArray>(responsePaths[i]).toString()
                val currentId = valueParser.read<JSONArray>("$..concepts[$i].id").toString()
                sortResult.expectIsLessRelevant(currentValue, i, currentId)
                expect(currentValue).to_contain(testString)
            }

        }

        @Test
        fun `expect prefLabel search with orgPath to return list of concepts from corresponding publisher only`() {
            val expOrgPath = "STAT/87654321/12345678".split("/")
            val result = apiGet("/concepts?prefLabel=$testString&size=100&orgPath=STAT/87654321/12345678")
            val resultOrgPath = jsonValueParser.parse(result).read<List<String>>("\$.._embedded.concepts.*.publisher.orgPath")

            resultOrgPath.forEach { expect(it).to_be_organisation(expOrgPath) }

        }

        @Test
        fun `expect prefLabel search  with trailing plus to return list of concepts sorted on exact matches in preferredLanguage `() {
            val result = apiGet("/concepts?prefLabel=$testString++++&size=100")
            val pathParser = jsonPathParser.parse(result)
            val valueParser = jsonValueParser.parse(result)
            val sortResult = SortResponse(sortWord = testString,
                    pathParser = pathParser)

            val responsePaths = sortResult.getPathsForField(jsonPath = prefLabelPath)


            for (i in responsePaths.indices) {
                val currentValue = valueParser.read<JSONArray>(responsePaths[i]).toString()
                val currentId = valueParser.read<JSONArray>("$..concepts[$i].id").toString()
                sortResult.expectIsLessRelevant(currentValue, i, currentId)
                expect(currentValue).to_contain(testString)
            }

        }
    }

    @Nested
    inner class OrgPathSearch {

        @Test
        fun `expect orgPath only search to return list of concepts from corresponding publisher only`() {
            val expOrgPath = "STAT/87654321/12345678".split("/")
            val result = apiGet("/concepts?size=100&orgPath=STAT/87654321/12345678&aggregations=orgPath")
            val resultOrgPath = jsonValueParser.parse(result).read<List<String>>("\$.._embedded.concepts.*.publisher.orgPath")
            resultOrgPath.forEach {
                expect(it).to_be_organisation(expOrgPath)
            }
        }

        @Test
        fun `expect orgPath only search with short path to return list of concepts from corresponding publisher only`() {
            val expOrgPath = "STAT/87654321".split("/")
            val result = apiGet("/concepts?size=100&orgPath=STAT/87654321")
            val resultOrgPath = jsonValueParser.parse(result).read<List<String>>("\$.._embedded.concepts.*.publisher.orgPath")
            resultOrgPath.forEach {
                expect(it).to_be_organisation(expOrgPath)
            }
        }

        @Test
        fun `expect orgPath only search with MISSING to return list of concepts from missing publisher only`() {
            val result = apiGet("/concepts?size=100&orgPath=MISSING")
            val resultOrgPath = jsonValueParser.parse(result).read<List<String>>("\$.._embedded.concepts.*.publisher.orgPath")
            assertNotEquals(resultOrgPath.size,0)
            resultOrgPath.forEach {
                assertNull(it)
            }
        }
    }

    @Nested
    inner class UrisOnlySearch {
        @Test
        fun `expect URIs search to return list of concepts that exactly match provided URIs`() {
            val conceptUris = jsonValueParser.parse(apiGet("/concepts?size=2")).read<List<String>>("$.._embedded.concepts.*.uri")

            val result = apiGet("/concepts?uris=${conceptUris.joinToString(",")}")
            val resultConceptUris = jsonValueParser.parse(result).read<List<String>>("$.._embedded.concepts.*.uri")

            assertEquals(2, resultConceptUris.size)
            assertTrue(resultConceptUris.containsAll(conceptUris))
        }
    }

    @Nested
    inner class IdentifiersOnlySearch {
        @Test
        fun `expect identifiers search to return list of concepts that exactly match provided identifers`() {
            val conceptIdentifiers = jsonValueParser.parse(apiGet("/concepts?size=2")).read<List<String>>("$.._embedded.concepts.*.identifier")

            val result = apiGet("/concepts?identifiers=${conceptIdentifiers.joinToString(",")}")
            val resultIdentifiers = jsonValueParser.parse(result).read<List<String>>("$.._embedded.concepts.*.identifier")

            assertEquals(2, resultIdentifiers.size)
            assertTrue(resultIdentifiers.containsAll(conceptIdentifiers))
        }
    }

    @Nested
    inner class GetWithId {}

}