package testUtils.assertions

import com.jayway.jsonpath.DocumentContext
import no.ccat.utils.LanguageProperties
import org.junit.jupiter.api.Assertions
import org.skyscreamer.jsonassert.JSONAssert
import testUtils.jsonPathParser
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit


class Expect(_result: Any?) {

    val result = _result

    fun to_equal(expected: String) {
        Assertions.assertEquals(expected, result)
    }
    fun to_equal(expected: List<String>) {
        result as List<String>
        Assertions.assertEquals(expected.size, result.size)
        expected.indices.forEach {
            val testString : String = when {
                it == 0 -> expected[it].substring(2)
                it ==  expected.size - 1 -> expected[it].substring(0, expected[it].length-3)
                else -> expected[it]
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

    fun to_contain(testString: String) {
        result as String
        Assertions.assertTrue(result.contains(testString), "expected to find $testString in result\n found $result")
    }

    fun to_equal(expected: Int) {
        Assertions.assertEquals(result,expected)
    }

    fun to_not_have_path(path: String) {
        jsonPathParser.parse(result).read<List<String>>(path)

    }

    fun to_be_organisation(expOrgPathParts: List<String>) {
        result as String
        val orgPathParts = result.split("/")

        orgPathParts.indices.forEach {
            Assertions.assertEquals(orgPathParts[it],expOrgPathParts[it])
        }
    }

    fun to_not_contain(expected: String) {
        result as String
        Assertions.assertFalse(result.contains(expected), "expected $result to not contain $expected")
    }

    fun to_not_contain(expected: String, key: String) {
        result as String
        Assertions.assertFalse(result.contains(expected), "expected query for $key to not contain $expected")
    }

    fun to_be_in_date_range(numberOfDays: Long) {
        result as String
        val formatter =  DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val date = LocalDate.parse(result,formatter)
        val now = LocalDate.now()
        val startDate = LocalDate.now().minus(numberOfDays+1,ChronoUnit.DAYS)
        Assertions.assertFalse(date.isAfter(now), "expected $date to not be after $now")
        Assertions.assertTrue(date.isAfter(startDate), "expected $date to be after $startDate")
    }

}