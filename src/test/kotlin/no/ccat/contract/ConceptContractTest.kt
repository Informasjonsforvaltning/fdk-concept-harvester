package no.ccat.contract

import com.jayway.jsonpath.PathNotFoundException
import net.minidev.json.JSONArray
import org.junit.jupiter.api.*
import testUtils.ApiTestContainer
import testUtils.SortResponse
import testUtils.assertions.apiGet
import testUtils.jsonPathParser
import testUtils.assertions.Expect as expect
import testUtils.jsonValueParser

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

        inner class  Aggregations {
            //TODO
        }

        @TestInstance(TestInstance.Lifecycle.PER_CLASS)
        @Nested
        inner class FullTextSearch {


            @Test
            fun `expect full text search to return list of concepts sorted on exact matches in preferredLanguage nb`(){

                val result = apiGet("/concepts?q=$testString&size=100")
                val pathParser = jsonPathParser.parse(result)
                val valueParser = jsonValueParser.parse(result)
                val sortResult = SortResponse(sortWord = testString,
                        pathParser = pathParser )

                val responsePaths = sortResult.getPathsForField(jsonPath = prefLabelPath)


                for (i in responsePaths.indices){
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
        }
    }
    @Nested
    inner class PrefLabelOnlySearch {
        @Test
        fun `expect prefLabel search to return list of concepts sorted on exact matches in preferredLanguage `(){
            val result = apiGet("/concepts?prefLabel=$testString&size=100")
            val pathParser = jsonPathParser.parse(result)
            val valueParser = jsonValueParser.parse(result)
            val sortResult = SortResponse(sortWord = testString,
                    pathParser = pathParser )

            val responsePaths = sortResult.getPathsForField(jsonPath = prefLabelPath)


            for (i in responsePaths.indices){
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
    }

    @Nested
    inner class GetWithId{}

}