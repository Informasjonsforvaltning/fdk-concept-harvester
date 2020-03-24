package no.ccat.contract

import net.minidev.json.JSONArray
import org.apache.jena.atlas.json.JSON
import org.apache.jena.atlas.json.JsonArray
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import testUtils.*
import testUtils.assertions.apiGet
import java.time.format.DateTimeFormatter
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
            fun `expect full text search to return list of concepts sorted on exact matches for multiple words`() {
                val multipleString = "arbeidsprosess i offentlig sektor"
                val urlString = "arbeidsprosess%20i%20offentlig%20sektor"
                val result = apiGet("/concepts?q=$urlString&size=100")
                val pathParser = jsonPathParser.parse(result)
                val valueParser = jsonValueParser.parse(result)
                val sortResult = SortResponse(sortWord = multipleString,
                        pathParser = pathParser)

                val responsePaths = sortResult.getPathsForField(jsonPath = prefLabelPath)


                for (i in responsePaths.indices) {
                    val currentValue = valueParser.read<JSONArray>(responsePaths[i])
                    val currentId = valueParser.read<JSONArray>("$..concepts[$i].id").toString()
                    sortResult.expectIsLessRelevant(currentValue.toString(), i, currentId)
                }
            }

            @Test
            fun `expect full text search to return list of concepts sorted on exact matches for multiple words with parenthesis`() {
                val multipleString = "arbeid (fritid)"
                val urlString = "arbeid%20(fritid)"
                val result = apiGet("/concepts?q=$urlString&size=100")
                val pathParser = jsonPathParser.parse(result)
                val valueParser = jsonValueParser.parse(result)
                val sortResult = SortResponse(sortWord = multipleString,
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
            fun `expect full text search with closed quotationmarks to return exact hit for one word`() {
                val oneWordString = "%22journal%22"
                val result = apiGet("/concepts?q=$oneWordString&size=100")
                val pathParser = jsonPathParser.parse(result)
                val valueParser = jsonValueParser.parse(result)
                val sortResult = SortResponse(sortWord = oneWordString,
                        pathParser = pathParser)

                val responsePaths = sortResult.getPathsForField(jsonPath = prefLabelPath)


                for (i in responsePaths.indices) {
                    val currentValue = valueParser.read<JSONArray>(responsePaths[i])
                    val currentId = valueParser.read<JSONArray>("$..concepts[$i].id").toString()
                    if (i == 0) {
                        val lang = getLanguage(jsonPathParser.parse(currentValue.toString()).read<List<String>>("$.*.*").toString())
                        val resultWord : String = jsonValueParser.parse(currentValue.toString()).read<JSONArray>("$[0]$lang")[0] as String
                        expect(resultWord).to_equal("journal")
                    }
                    sortResult.expectIsLessRelevant(currentValue.toString(), i, currentId)
                }
            }


            @Test
            fun `expect full text search with orgPath to return list of concepts from corresponding publisher only`() {
                val result = apiGet("/concepts?q=$testString&size=100&orgPath=/STAT/87654321/12345678")

                val expOrgPath = "/STAT/87654321/12345678".split("/")
                val resultOrgPath = jsonValueParser.parse(result).read<List<String>>("\$.._embedded.concepts.*.publisher.orgPath")

                resultOrgPath.forEach { expect(it).to_be_organisation(expOrgPath) }
            }

            @Test
            fun `expect full text search with orgPath and trailing plus chars to return list of concepts from corresponding publisher only`() {
                val result = apiGet("/concepts?q=$testString++&size=100&orgPath=/STAT/87654321/12345678")

                val expOrgPath = "/STAT/87654321/12345678".split("/")
                val resultOrgPath = jsonValueParser.parse(result).read<List<String>>("\$.._embedded.concepts.*.publisher.orgPath")

                resultOrgPath.forEach { expect(it).to_be_organisation(expOrgPath) }
            }

            @Test
            fun `expect full text search with special chars in query to not throw error`() {
                val openParenthesisString = "Some(thing"
                val closedParenthesisString = "S(omething)"
                val openQuoteString = "%22arbeid"
                val closedQuoteString = "%22arbeid%20fritid%22"
                val questionStartString = "%3FstratingWith"
                val questionInString = "strati%3FngWith"
                val questionEndString = "stratingWith%3F"
                val mayhem = "has%2F%2Bsgh%21jh%26ff4%29%29%23%2256%28%29asf%5Basf%5Dasf%7C%7Cas%7B%7Dasfa%21%21"

                val result = mapOf<String,String>(
                        openParenthesisString to apiGet("/concepts?q=$openParenthesisString"),
                        closedParenthesisString to apiGet("/concepts?q=$closedParenthesisString"),
                        openQuoteString to apiGet("/concepts?q=$openQuoteString"),
                        closedQuoteString to apiGet("/concepts?q=$closedQuoteString"),
                        questionEndString to apiGet("/concepts?q=$questionEndString"),
                        questionInString to apiGet("/concepts?q=$questionStartString"),
                        questionEndString to apiGet("/concepts?q=$questionInString"),
                        mayhem to apiGet("/concepts?q=$mayhem")
                )

                result.forEach {
                    expect(it.value).to_not_contain("error",it.key)
                }
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
            val expOrgPath = "/STAT/87654321/12345678".split("/")
            val result = apiGet("/concepts?prefLabel=$testString&size=100&orgPath=/STAT/87654321/12345678")
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
            val expOrgPath = "/STAT/87654321/12345678".split("/")
            val result = apiGet("/concepts?size=100&orgPath=/STAT/87654321/12345678&aggregations=orgPath")
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
    inner class verifyUriFromFdkWorking {
        @Test
        fun `expect returnvalues for conceptsearch in regiastration soloution to be correct`() {
            val searchString = "arkiv"
            val result = apiGet("/concepts?prefLabel=$searchString&returnfields=uri,definition.text,publisher.prefLabel,publisher.name&size=25")
            val pathParser = jsonPathParser.parse(result)
            val valueParser = jsonValueParser.parse(result)
            val sortResult = SortResponse(sortWord = searchString,
                    pathParser = pathParser)
            val responsePaths = sortResult.getPathsForField(jsonPath = prefLabelPath)


            for (i in responsePaths.indices) {
                val currentValue = valueParser.read<JSONArray>(responsePaths[i]).toString()
                val currentId = valueParser.read<JSONArray>("$..concepts[$i].id").toString()
                sortResult.expectIsLessRelevant(currentValue, i, currentId)
                expect(currentValue).to_contain(searchString)
            }
        }

        @Test
        fun `expect returnvalues for conceptsearch in fdk portal to be correct`() {
            val searchString = "dokument"
            val result = apiGet("/concepts?q=$testString&size=100&orgPath=%2FSTAT%2F87654321%2F12345678")
            val pathParser = jsonPathParser.parse(result)
            val valueParser = jsonValueParser.parse(result)
            val sortResult = SortResponse(sortWord = searchString,
                    pathParser = pathParser)
            val responsePaths = sortResult.getPathsForField(jsonPath = prefLabelPath)

            val expOrgPath = "/STAT/87654321/12345678".split("/")
            val resultOrgPath = jsonValueParser.parse(result).read<List<String>>("\$.._embedded.concepts.*.publisher.orgPath")

            resultOrgPath.forEach { expect(it).to_be_organisation(expOrgPath) }

            for (i in responsePaths.indices) {
                val currentValue = valueParser.read<JSONArray>(responsePaths[i]).toString()
                val currentId = valueParser.read<JSONArray>("$..concepts[$i].id").toString()
                sortResult.expectIsLessRelevant(currentValue, i, currentId)
            }
        }

        @Disabled
        @Test
        fun `expect query to return concepts harvested in the last week`() {
           val result =  apiGet("/concepts?firstHarvested=7&size=100");
           val dates = jsonPathParser.parse(result).read<JSONArray>("$.._embedded.concepts.*.harvest.firstHarvested")

            //jsonValueParser.parse(result).read<JSONArray>("$.._embedded.concepts.*.harvest.firstHarvested")[0].toString().split("T")[0],
            dates.forEach {
                expect((jsonValueParser.parse(result).read<JSONArray>(it.toString())[0] as CharSequence).split("T")[0]).to_be_in_date_range(7)
            }

        }

        @Disabled
        @Test
        fun `expect query to return concepts harvested in the last month`() {
            val total = apiGet("/count")
            val result =  apiGet("/concepts?firstHarvested=30&size=100");
            val dates = jsonPathParser.parse(result).read<JSONArray>("$.._embedded.concepts.*.harvest.firstHarvested")
            val hits = jsonValueParser.parse(result).read<JSONArray>("$..page.totalElements")[0];
            assertEquals(total.toInt(),hits)
            dates.forEach {
                expect((jsonValueParser.parse(result).read<JSONArray>(it.toString())[0] as CharSequence).split("T")[0]).to_be_in_date_range(30)
            }

        }
    }


    @Nested
    inner class GetWithId {}

}