package no.ccat.contract

import com.google.gson.JsonArray
import org.junit.jupiter.api.*
import testUtils.ApiTestContainer
import testUtils.SortResponse
import testUtils.jsonPathParser
import testUtils.assertions.Expect as expect
import testUtils.jsonValueParser

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Tag("contract")

class ConceptContractTest : ApiTestContainer() {


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
            lateinit var result: String;
            val prefLabelPath = "$..concepts.prefLabel"
            val testString = "dokument"
            @BeforeAll
                fun setup(){
                result = "placeholder http request"
            }


            @Test
            fun `expect full text search to return list of concepts sorted on exact matches in preferredLanguage nb`(){
                val prefLabelPaths = jsonPathParser.parse(result).read<List<String>>(prefLabelPath);
                val valueParser = jsonValueParser.parse(result)
                val sortResult = SortResponse(sortWord = "dokument");


                for (i in prefLabelPaths.indices){
                    val currentValue = valueParser.read<String>(prefLabelPaths[i])
                    val currentId = valueParser.read<JsonArray>("$..concept[$i]").toString()
                    expect(sortResult.isLessRelevant(prefLabelPaths[i], currentValue, i, currentId)).to_be_true()
                }
            }
        }

        inner class PrefLabelOnlySearch {
            @Test
            fun `expect prefLabel search to return list of concepts sorted on exact matches in preferredLanguage `(){

            }

            //TODO specifications for behaviour of prefLabel search

        }

    }

    @Nested
    inner class GetWithId{}

}