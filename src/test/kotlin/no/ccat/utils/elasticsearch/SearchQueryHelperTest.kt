package no.ccat.utils.elasticsearch

import org.junit.jupiter.api.*
import utils.LanguageProperties
import utils.LanguageUtils
import testUtils.assertions.Expect as expect

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Tag("unit")

class SearchQueryHelperTest {
    @Nested
    class LanguagePropertiesTest{

        @Test
        fun `empty constructor should return a objbect for nb `(){
            val key = "nb";
            val interpreter = "norwegian"

            val result = LanguageProperties()

            expect(result.key).to_equal(key)
            expect(result.interpreter).to_equal(interpreter)
            expect(result.stemmer).to_be_null()
        }

        @Test
        fun `constructor with params should return an object for specified language `(){
            val key = "en"
            val interpreter = "english"
            val stemmer = "custom_stemmer"

            val result = LanguageProperties(key, interpreter, stemmer)

            expect(result.key).to_equal(key)
            expect(result.interpreter).to_equal(interpreter)
            expect(result.stemmer).to_equal(stemmer)
        }

    }
    @Nested
    class LanguageUtilsTest {
        @Nested
        class GetLanguage{
            var languageUtils: LanguageUtils = LanguageUtils();

            @Test
            fun `should return a nn object when language is set to nn`(){
                val expectedObject : LanguageProperties = LanguageProperties(key = "nn")
                val result = languageUtils.getLanguage("nn")
                expect(result).to_equal(expectedObject);
            }

            @Test
            fun `should return an array containg en and nb when language is set to nn`(){
                val expectedKey1 = "nb"
                val expectedkey2 = "en"

                val result = languageUtils.getSecondaryLanguage("nn");
                expect(result).to_contain(expectedKey1,expectedkey2)

            }
        }
    }

}