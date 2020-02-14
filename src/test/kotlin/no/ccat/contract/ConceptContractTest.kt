package no.ccat.contract

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
    val prefLabelPath = "$.._embedded.concepts.*.prefLabel"
    val testString = "arkiv"


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

                val responsePaths = sortResult.getPathsForField(jsonPath = prefLabelPath, field = "prefLabel")


                for (i in responsePaths.indices){
                    val currentValue = valueParser.read<JSONArray>(responsePaths[i])
                    val currentId = valueParser.read<JSONArray>("$..concepts[$i].id").toString()
                    expect(sortResult.isLessRelevant(responsePaths[i], currentValue.toString(), i, currentId)).to_be_true()
                }
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

            val responsePaths = sortResult.getPathsForField(jsonPath = prefLabelPath, field = "prefLabel")


            for (i in responsePaths.indices){
                val currentValue = valueParser.read<JSONArray>(responsePaths[i]).toString()
                val currentId = valueParser.read<JSONArray>("$..concepts[$i].id").toString()
                expect(sortResult.isLessRelevant(responsePaths[i], currentValue, i, currentId)).to_be_true()
                expect(currentValue).to_contain(testString)
            }

        }

        //TODO specifications for behaviour of prefLabel search

    }

    @Nested
    inner class GetWithId{}

}