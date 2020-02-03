package testUtils.assertions

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assumptions
import utils.LanguageProperties


class Expect(_result: Any?){

    val result = _result

    fun to_equal(expected: String) {
        Assertions.assertEquals(expected,result)
    }
    fun to_equal(expected: Int) {
        Assertions.assertEquals(expected,result)
    }

    fun to_equal(expected: LanguageProperties) {
        when(result){
            is LanguageProperties -> {
                Assertions.assertEquals(expected.key, result.key)
                Assertions.assertEquals(expected.interpreter, result.interpreter)
                Assertions.assertEquals(expected.stemmer,result.stemmer)
            }
        }
    }

    fun to_be_null(){
        Assertions.assertNull(result)
    }
    fun to_contain(expected: String) {
        when(result) {
            is String -> Assertions.assertTrue(result.contains(expected), "expected string to contain $expected")
            is LinkedHashMap<*, *> -> Assertions.assertTrue(result.contains(expected))
            else -> throw AssertionError("Unexpected datatype in result");
        }
    }

    fun to_contain(expectedKey: String, expectedKey2: String){
        result as Array<LanguageProperties>
        val resultKey1 = result[0].key
        val resultKey2 = result[1].key

        val hasFirsLanguage = resultKey1 == expectedKey || resultKey1 == expectedKey2
        val hasSecondLanguage = resultKey2 == expectedKey || resultKey2 == expectedKey2

        Assertions.assertTrue(hasFirsLanguage)
        Assertions.assertTrue(hasSecondLanguage)
    }

    fun to_be_true(){
        Assertions.assertTrue(result as Boolean);
    }
}

fun assume_authenticated(status: String) {
    Assumptions.assumeFalse(status.equals("401"))
}

fun assume_success(status: String) {
    Assumptions.assumeTrue(status.equals("201"))
}

fun assume_implemented(status: String){
    Assumptions.assumeFalse(status.equals("501"))
}