package no.ccat.utils.elasticsearch
import no.ccat.utils.LanguageProperties
import no.ccat.utils.buildSearchStringInPrefLabelBoost
import no.ccat.utils.buildExactMatchString
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import testUtils.COMPLEX_SEARCH_STRING
import java.io.File
import testUtils.assertions.Expect as expect

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Tag("unit")

class SearchQueryBuilderTest {

    @Nested
    inner class getExactMatchStringTest
    {
        @Test
        fun`should return correct searchstring for prefered language nb`(){
            val searchString = "ark"
            val expected = "(doc['prefLabel.nb'][0].contains('$searchString') && doc['prefLabel.nb'].size() == 1 && doc['prefLabel.nb'][0].length() < 7)"
            val result = buildExactMatchString("nb",searchString,7);
            expect(result).to_equal(expected)
        }

        @Test
        fun`should return correct searchstring for prefered language en`(){
            val searchString = "another thing"
            val expected = "(doc['prefLabel.en'][0].contains('$searchString') && doc['prefLabel.en'].size() == 1 && doc['prefLabel.en'][0].length() < 7)"
            val result = buildExactMatchString("en",searchString, 7);
            expect(result).to_equal(expected)
        }
    }
    @Nested
    inner class buildSearchStringInPrefLabelBoostTest
    {
        @Test
        fun `should return a builder containing 4 simple_query_string for preflabel buidlers`(){


        }
    }
    @Nested
    inner class buildMatchPhrasePrefixBoostTest
    {
        val expectedPrimary = File("./src/test/resources/elasticsearch/phrase_prefix_complex_nb.json").readText(Charsets.UTF_8)
        val expectedSecondary = File("./src/test/resources/elasticsearch/phrase_prefix_complex_nn_secondary.json").readText(Charsets.UTF_8)

        @Test
        fun `should return a builder containing 4 match_phrase_prefix buidlers`(){
            val lang = LanguageProperties();
            val result = buildSearchStringInPrefLabelBoost(COMPLEX_SEARCH_STRING,lang)
            expect(result[0].toString()).json_to_equal(expectedPrimary)
            expect (result[1].toString()).json_to_equal(expectedSecondary)
        }
    }
}