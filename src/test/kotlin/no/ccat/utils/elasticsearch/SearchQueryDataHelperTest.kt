package no.ccat.utils.elasticsearch

import no.ccat.utils.*
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import testUtils.*
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
             expect(result).to_contain(expected);
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

            @Test
            fun `should return a nn object when language is set to nn`(){
                val expectedObject : LanguageProperties = LanguageProperties(key = "nn")
                val result = language("nn")

                expect(result).to_equal(expectedObject);
            }

            @Test
            fun `should return an array containg en,nb and no when language is set to nn`(){
                val expected = arrayOf(LanguageProperties("nb"),LanguageProperties("en", analyzer = "english"),LanguageProperties("no"))
                val result = secondaryLanguages("nn");
                expect(result).to_contain(expected)
            }
        }
    }

    @Nested
    inner class QueryParamsTest{
        @Nested
        inner class queryType {

            @Test
            fun `should return matchAllSearch if no query value is present`() {
                val params = QueryParams();
                val paramsWithSort = QueryParams(sortDirection = "asc");
                val paramsWithReturnFields = QueryParams(returnFields = "prefLabel");
                val paramsWithAggregations = QueryParams(aggregation = "orgPath");
                val paramsWithSortField = QueryParams(sortField = "prefLabel");
                val paramsWithSize = QueryParams(size = "10")
                val paramsWithPage = QueryParams(startPage = "2")
                val paramsWithPageAndSort = QueryParams(startPage = "2", sortDirection = "asc")

                assertEquals(QueryType.matchAllSearch,params.queryType)
                assertEquals(QueryType.matchAllSearch,paramsWithSort.queryType)
                assertEquals(QueryType.matchAllSearch,paramsWithPage.queryType)
                assertEquals(QueryType.matchAllSearch,paramsWithReturnFields.queryType)
                assertEquals(QueryType.matchAllSearch,paramsWithAggregations.queryType)
                assertEquals(QueryType.matchAllSearch,paramsWithSortField.queryType)
                assertEquals(QueryType.matchAllSearch,paramsWithSize.queryType)
                assertEquals(QueryType.matchAllSearch,paramsWithPageAndSort.queryType)
            }

            @Test
            fun `should return queryStringSearch for ambigious search query params`() {
                assertEquals(QueryType.queryStringSearch, paramsWithQueryStringAndIdentifiers().queryType)
                assertEquals(QueryType.queryStringSearch, paramsWithQueryStringAndUris().queryType)
                assertEquals(QueryType.queryStringSearch, paramsWithQueryStringAndPrefLabel().queryType)
                assertEquals(QueryType.queryStringSearch, paramsWithQueryStringAndUris().queryType)
                assertEquals(QueryType.queryStringSearch, paramsWithAllQueryValues().queryType)

            }

            @Test
            fun `should return queryStringSearchWithOrgPath`() {
                assertEquals(QueryType.queryStringSearchWithOrgPath, paramsWithQueryStringAndOrgPath().queryType)
                assertEquals(QueryType.queryStringSearchWithOrgPath, paramsWithQueryStringUriAndOrgPath().queryType)
                assertEquals(QueryType.queryStringSearchWithOrgPath, paramsWithQueryStringAndIdentifiersAndOrgPath().queryType)

            }

            @Test
            fun `should return preflabelSearch`() {
                assertEquals(QueryType.prefLabelSearch, paramsWithPrefLabel().queryType)
                assertEquals(QueryType.prefLabelSearch, paramsWithPrefLabelAndUris().queryType)
                assertEquals(QueryType.prefLabelSearch, paramsWithPrefLabelUrisAndSort().queryType)
                assertEquals(QueryType.prefLabelSearch, paramsWithPrefLabelIdentifiersAndSort().queryType)
            }
            @Test
            fun `should return preflabelSearchWithOrgPath`() {
                assertEquals(QueryType.prefLabelSearcgWithOrgPath, paramsWithPrefLabelUrisAndOrgPath().queryType)
                assertEquals(QueryType.prefLabelSearcgWithOrgPath, paramsWithPrefLabelAndOrgPath().queryType)
                assertEquals(QueryType.prefLabelSearcgWithOrgPath, paramsWithPrefLabelAndOrgPathAndSort().queryType)
            }

            @Test
            fun `should return urisSearch`() {
                assertEquals(QueryType.urisSearch, paramsWithUri().queryType)
                assertEquals(QueryType.urisSearch, paramsWithUriAndSize().queryType)
            }

            @Test
            fun `should return identifiersSearch`() {
                assertEquals(QueryType.identifiersSearch, paramsWithIdentifiers().queryType)
                assertEquals(QueryType.identifiersSearch, paramsWithIdentifiersAndPage().queryType)
            }
            @Test
            fun `should return orgPathOnly`() {
                assertEquals(QueryType.orgPathOnlySearch, QueryParams(orgPath = "gadshfgasjf").queryType)
                assertEquals(QueryType.orgPathOnlySearch, QueryParams(orgPath = "gadshfgasjf", aggregation = "hjaskhfjkasf").queryType)
            }


        }

        @Nested
        inner class isStringQuerySearch {
            @Test
            fun `should return true if queryString has a value`() {
               assertTrue(paramsWithQueryString().isQueryStringSearch())
            }
            @Test
            fun `should return true if queryString and prefLabel had value`() {
                assertTrue(paramsWithQueryStringAndPrefLabel().isQueryStringSearch())
            }

            @Test
            fun `should return true if queryString and identifiers has value`() {
                assertTrue(paramsWithQueryStringAndIdentifiers().isQueryStringSearch())
            }

            @Test
            fun `should return true if queryString and uris has value`() {
                assertTrue(paramsWithQueryStringAndUris().isQueryStringSearch())

            }

            @Test
            fun `should return true if all search query fields has value`() {
                assertTrue(paramsWithAllQueryValues().isQueryStringSearch())
            }

        }

        @Nested
        inner class isPrefLabelSearch {
            @Test
            fun `should return true if prefLabel has value`() {
                assertTrue(paramsWithPrefLabel().isPrefLabelSearch())
            }

            @Test
            fun `should return true if prefLabel and uris has value`() {
                assertTrue(paramsWithPrefLabelAndUris().isPrefLabelSearch())
            }

        }

        @Nested
        inner class isTextSearch {
            @Test
            fun `should return true when preflabel or queryString is present`() {
                assertTrue(paramsWithPrefLabel().isTextSearch())
                assertTrue(paramsWithPrefLabelAndUris().isTextSearch())
                assertTrue(paramsWithQueryString().isTextSearch())
                assertTrue(paramsWithQueryStringAndIdentifiers().isTextSearch())
                assertTrue(paramsWithAllQueryValues().isTextSearch())
                assertTrue(paramsWithQueryStringIdentifiersAndSort().isTextSearch())
            }

            @Test
            fun `should return false when neither preflabel or queryString is present`() {
                assertFalse(paramsWithIdentifiers().isTextSearch())
                assertFalse(paramsWithUri().isTextSearch())
                assertFalse(paramsWithUriAndIdentifiers().isTextSearch())
            }
        }

        @Nested
        inner class isIdSearch {
            @Test
            fun `should return true if uri or identifier and no querystring values are present`(){
                assertTrue(paramsWithUri().isIdSearch())
                assertTrue(paramsWithUriAndIdentifiers().isIdSearch())
                assertTrue(paramsWithUri().isIdSearch())

            }
            @Test
            fun `should return false if uri or identifier and  querystring values are present`(){
                assertFalse(paramsWithPrefLabelAndUris().isIdSearch())
                assertFalse(paramsWithQueryStringAndIdentifiers().isIdSearch())
                assertFalse(paramsWithAllQueryValues().isIdSearch())
            }


        }

        @Nested
        inner class isEmpty {
            @Test
            fun `should return true if all values are default`() {
                val params = QueryParams();
                 expect(params.isEmpty()).to_be_true()
            }

            @Test
            fun `should return false if all values are not default`() {
                val params = QueryParams(queryString = "nsakfa", orgPath = "//13638/basfkj", size = "500");
                expect(params.isEmpty()).to_be_false()
            }
        }

        @Nested
        inner class isEmptyQuerySearch {
            @Test
            fun `should return true if search params are default`() {
                val params = QueryParams(size = "78");
                expect(params.isEmptySearchQuery()).to_be_true()
            }

            @Test
            fun `should return false if orgPath is not default `() {
                val params = QueryParams(orgPath = "/13638/basfkj");
                expect(params.isEmptySearchQuery()).to_be_false()

            }
            @Test
            fun `should return false if searchString is not default `() {
                val params = QueryParams(queryString = "nsakfa");
                expect(params.isEmpty()).to_be_false()

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

        @Nested
        inner class shouldFilterOnOrgPath {

            @Test
            fun `should filter on orgPath`(){
                assertTrue(paramsWithQueryStringAndOrgPath().shouldfilterOnOrgPath())
                assertTrue(paramsWithQueryStringUriAndOrgPath().shouldfilterOnOrgPath())
                assertTrue(paramsWithQueryStringUriAndOrgPath().shouldfilterOnOrgPath())
                assertTrue(paramsWithQueryStringPrefLabelAndOrgPath().shouldfilterOnOrgPath())
            }

            fun `should not filter on orgPath`(){
                assertFalse(paramsWithQueryString().shouldfilterOnOrgPath())
                assertFalse(paramsWithQueryStringAndUris().shouldfilterOnOrgPath())
                assertFalse(paramsWithQueryStringAndPrefLabel().shouldfilterOnOrgPath())
            }
        }

        @Nested
        inner class isFilerOnPathgOnly{
            @Test
            fun `should filter on orgPath`(){
                assertFalse(paramsWithQueryStringAndOrgPath().isOrgPathOnly())
                assertFalse(paramsWithQueryStringUriAndOrgPath().isOrgPathOnly())
                assertFalse(paramsWithQueryStringUriAndOrgPath().isOrgPathOnly())
                assertFalse(paramsWithQueryStringPrefLabelAndOrgPath().isOrgPathOnly())
                assertTrue(QueryParams(orgPath = "ANNET/123567").isOrgPathOnly())
            }
        }

        @Nested
        inner class sanitizeString {
            @Test
            fun `should remove trailing plus and whitespace in prefLabel`(){
                val expected = QueryParams(prefLabel = "something")
                val result = QueryParams(prefLabel = "something+").sanitizeQueryStrings()
                val resultWithWhiteSpace = QueryParams(prefLabel = "something       ").sanitizeQueryStrings()
                val resultWithWhiteSpaceAndPlus = QueryParams(prefLabel = "something       + ").sanitizeQueryStrings()

                assertEquals(expected, result);
                assertEquals(expected, resultWithWhiteSpace);
                assertEquals(expected, resultWithWhiteSpaceAndPlus);
            }

            @Test
            fun `should remove preceding plus and whitespace in prefLabel`(){
                val expected = QueryParams(prefLabel = "something")
                val result = QueryParams(prefLabel = "++something").sanitizeQueryStrings()
                val resultWithWhiteSpace = QueryParams(prefLabel = "    something").sanitizeQueryStrings()
                val resultWithWhiteSpaceAndPlus = QueryParams(prefLabel = "    + something").sanitizeQueryStrings()

                assertEquals(expected, result);
                assertEquals(expected, resultWithWhiteSpace);
                assertEquals(expected, resultWithWhiteSpaceAndPlus);
            }
 @Test
            fun `should remove trailing plus and whitespace in queryString`(){
                val expected = QueryParams(queryString = "something")
                val result = QueryParams(queryString = "something+").sanitizeQueryStrings()
                val resultWithWhiteSpace = QueryParams(queryString = "something       ").sanitizeQueryStrings()
                val resultWithWhiteSpaceAndPlus = QueryParams(queryString = "something       + ").sanitizeQueryStrings()

                assertEquals(expected, result);
                assertEquals(expected, resultWithWhiteSpace);
                assertEquals(expected, resultWithWhiteSpaceAndPlus);
            }

            @Test
            fun `should remove preceding plus and whitespace in queryString`(){
                val expected = QueryParams(queryString = "something")
                val result = QueryParams(queryString = "++something").sanitizeQueryStrings()
                val resultWithWhiteSpace = QueryParams(queryString = "    something").sanitizeQueryStrings()
                val resultWithWhiteSpaceAndPlus = QueryParams(queryString = "    + something").sanitizeQueryStrings()

                assertEquals(expected, result);
                assertEquals(expected, resultWithWhiteSpace);
                assertEquals(expected, resultWithWhiteSpaceAndPlus);
            }


            @Test
            fun `should not remove preceding forward slash from orgPath prefLabel search`(){
                val expected = QueryParams(prefLabel = "something", orgPath = "/STAT/16465799").sanitizeQueryStrings()
                val result = QueryParams(prefLabel = "something +", orgPath = "/STAT/16465799").sanitizeQueryStrings()
                assertEquals(expected, result);
            }

            @Test
            fun `should not remove preceding forward slash from orgPath queryString search`(){
                val expected = QueryParams(queryString = "something +", orgPath = "/STAT/16465799").sanitizeQueryStrings()
                val result = QueryParams(queryString = "something +", orgPath = "/STAT/16465799").sanitizeQueryStrings()
                assertEquals(expected, result);
            }


        }
    }
}