package testUtils.assertions

import com.jayway.jsonpath.DocumentContext
import no.ccat.utils.LanguageProperties
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assumptions
import org.skyscreamer.jsonassert.JSONAssert
import java.lang.StringBuilder


class Expect(_result: Any?) {

    val result = _result

    fun to_equal(expected: String) {
        Assertions.assertEquals(expected, result)
    }
    fun to_equal(expected: List<String>) {
        result as List<String>
        Assertions.assertEquals(expected.size, result.size)
        for(i in expected.indices){
            var testString = expected[i]
            if(i == 0) {
               testString = testString.substring(2)
            }
            if(i == expected.size - 1) {
                testString = testString.substring(0, testString.length-3)
            }

            Assertions.assertTrue(scriptListContains(testString), "expected to find\n $testString \n found ${resultToString(result)}")
        }
    }

    fun to_equal(expected: LanguageProperties) {
        when (result) {
            is LanguageProperties -> {
                Assertions.assertEquals(expected.key, result.key)
                Assertions.assertEquals(expected.analyzer, result.analyzer)
                Assertions.assertEquals(expected.stemmer, result.stemmer)
            }
        }
    }

    fun json_to_equal(expected: String) {
        JSONAssert.assertEquals(expected, result as String, false)
    }

    fun to_be_null() {
        Assertions.assertNull(result)
    }

    fun to_contain(expected: List<Any>) {
        val listResult = result as List<Any>

        Assertions.assertEquals(expected.size, listResult.size)
        Assertions.assertTrue(listResult.contains(expected))

    }

    fun to_contain(expected: Array<LanguageProperties>) {
        result as Array<LanguageProperties>

        Assertions.assertEquals(result.size, expected.size)
        for (expProperties in expected) {
            Assertions.assertTrue(result.contains(expProperties), "$expProperties is not in list")
        }


    }

    fun to_be_true() {
        Assertions.assertTrue(result as Boolean);
    }

    fun to_be_false() {
        Assertions.assertFalse(result as Boolean);
    }


    fun json_to_have_entries_like(expected: DocumentContext){
        result as DocumentContext
        val expPaths = removeIndexes(expected.read<List<String>>("$..*"))
        val resultPaths = removeIndexes(result.read<List<String>>("$..*"))

        for (expPath in expPaths) {
            Assertions.assertTrue(resultPaths.contains(expPath), "\n$expPath was not found in queryList\n${resultToString(resultPaths)}")
        }
    }

    private fun resultToString(resultList: List<String>) : String {
        val builder = StringBuilder();
        for(string in resultList){
            builder.append("\n$string")
        }
        return builder.toString()
    }

    private fun removeIndexes(resultList: List<String>): List<String> {
        val removedIndexes = mutableListOf<String>()
        val pattern = "\\d+".toRegex()
        for (i in resultList.indices){
            val replaced = resultList[i].replace(pattern,"");
            removedIndexes.add(replaced)
        }
        return removedIndexes;
    }

    private fun scriptListContains(string: String): Boolean {
        result as List<String>
        val trimmedTestString = string.trim();
        for (resultString in result) {
              if(resultString.contains(trimmedTestString))
              return true
        }
        return false
    }

    fun to_be_less_exact_than(lastHit: String) {

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