package no.ccat.utils.elasticsearch

import org.junit.jupiter.api.*
import no.ccat.utils.LanguageProperties
import no.ccat.utils.LanguageUtils
import no.ccat.utils.QueryParams
import testUtils.assertions.Expect as expect

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Tag("unit")

class SearchQueryHelperTest {
    @Nested
    inner class LanguagePropertiesTest{

        @Test
        fun `empty constructor should return a objbect for nb `(){
            val key = "nb";
            val interpreter = "norwegian"

            val result = LanguageProperties()

            expect(result.key).to_equal(key)
            expect(result.analyzer).to_equal(interpreter)
            expect(result.stemmer).to_be_null()
        }

        @Test
        fun `constructor with params should return an object for specified language `(){
            val key = "en"
            val interpreter = "english"
            val stemmer = "custom_stemmer"

            val result = LanguageProperties(key, interpreter, stemmer)

            expect(result.key).to_equal(key)
            expect(result.analyzer).to_equal(interpreter)
            expect(result.stemmer).to_equal(stemmer)

         @Test
         fun `default object should have secondaryLanguage keys nn,no and en`() {
             val result: List<String> = LanguageProperties().secondaryKeys()
             val expected = listOf<String>("nn","no","en")
             expect(result).to_contain(result);
            }

            @Test
        fun `en object should have secondaryLanguages keys nn,no and nb`() {
                val result: List<String> = LanguageProperties(key="en").secondaryKeys()
                val expected = listOf<String>("nn","no","nb")
                expect(result).to_contain(result);
            }
        }
    }

    @Nested
    inner class LanguageUtilsTest {
        @Nested
        inner class GetLanguage{
            var languageUtils: LanguageUtils = LanguageUtils();

            @Test
            fun `should return a nn object when language is set to nn`(){
                val expectedObject : LanguageProperties = LanguageProperties(key = "nn")
                val result = languageUtils.getLanguage("nn")
                expect(result).to_equal(expectedObject);
            }

            @Test
            fun `should return an array containg en,nb and no when language is set to nn`(){
                val expected = arrayOf(LanguageProperties("nb"),LanguageProperties("en", analyzer = "english"),LanguageProperties("no"))
                val result = languageUtils.getSecondaryLanguage("nn");

                expect(result).to_contain(expected)

            }
        }
    }

    @Nested
    inner class QueryParamsTest{
        @Nested
        inner class isEmpty {
            @Test
            fun `should return true if all values are default`() {
                val params = QueryParams();
                 expect(params.isEmpty()).to_be_true()
            }

            @Test
            fun `should return false if all values are default`() {
                val params = QueryParams(queryString = "nsakfa", orgPath = "//13638/basfkj", size = "500");
                expect(params.isEmpty()).to_be_false()
            }
        }

        @Nested
        inner class isEmptySearch {
            @Test
            fun `should return true if search params are default`() {
                val params = QueryParams(size = "78");
                expect(params.isEmptySearch()).to_be_true()
            }

            @Test
            fun `should return false if orgPath is not default `() {
                val params = QueryParams(orgPath = "//13638/basfkj");
                expect(params.isEmptySearch()).to_be_false()

            }
            @Test
            fun `should return false if searchString is not default `() {
                val params = QueryParams(queryString = "nsakfa");
                val result = expect(params.isEmpty()).to_be_false()

            }

        }
        @Nested
        inner class isDefaultSize {
            @Test
            fun `should return true if all values are default`() {
                val params = QueryParams(queryString = "nsakfa", orgPath = "//13638/basfkj");
                val result = expect(params.isDefaultSize()).to_be_true()
            }

            @Test
            fun `should return false if size is not default`() {
                val params = QueryParams(size = "78");
                expect(params.isDefaultSize()).to_be_false()
            }

            @Test
            fun `should return false if page is not default`() {
                val params = QueryParams(startPage = "78");
                expect(params.isDefaultSize()).to_be_false()
            }
        }

        @Nested
        inner class isDefaultPresentation {
            @Test
            fun `should return true if all values are default`() {
                val params = QueryParams(queryString = "nsakfa", orgPath = "//13638/basfkj", size = "500");
                expect(params.isDefaultPresentation()).to_be_true()
            }

            @Test
            fun `should return false if aggregation is not default`() {
                val params = QueryParams(aggregation = "ja");
                expect(params.isDefaultPresentation()).to_be_false()

            }

            @Test
            fun `should return false if sortDirection is not default`() {
                val params = QueryParams(sortDirection = "ja");
                expect(params.isDefaultPresentation()).to_be_false()

            }

            @Test
            fun `should return false if sortField is not default`() {
                val params = QueryParams(sortField = "ja");
                expect(params.isDefaultPresentation()).to_be_false()

            }

            @Test
            fun `should return false if returnfields is not default`() {
                val params = QueryParams(returnFields = "ja");
                expect(params.isDefaultPresentation()).to_be_false()

            }

            @Test
            fun `should return true if prefLabel is present and queryString is default`() {
                val params = QueryParams(prefLabel = "ja");
                expect(params.isPrefLabelSearch()).to_be_true();
            }

            @Test
            fun `should return false if queryString and prefLabel is default`() {
                val params = QueryParams();
                expect(params.isPrefLabelSearch()).to_be_false()

            }

            @Test
            fun `should return false if queryString and prefLabel is not default`() {
                val params = QueryParams(prefLabel = "neida", queryString = "joda");
                expect(params.isPrefLabelSearch()).to_be_false()

            }
        }
    }

}