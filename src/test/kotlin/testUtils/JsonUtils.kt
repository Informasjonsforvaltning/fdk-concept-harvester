package testUtils

import com.jayway.jsonpath.*
import net.minidev.json.JSONArray
import no.ccat.utils.allLanguages
import org.junit.jupiter.api.fail

val jsonPathParser = JsonPath
        .using(Configuration.builder()
                .options(Option.AS_PATH_LIST, Option.DEFAULT_PATH_LEAF_TO_NULL)
                .build())

val jsonValueParser = JsonPath
        .using(Configuration.builder()
                .options(Option.ALWAYS_RETURN_LIST)
                .options(Option.DEFAULT_PATH_LEAF_TO_NULL)
                .build())



class SortResponse(val sortWord: String,
                   val primaryLanguagePath: String = "nb",
                   val pathParser: DocumentContext) {

    var conceptIds = mutableListOf<String>()
    var lastExactMatch = -1
    var lastLangPath = ""

    fun expectIsLessRelevant(currentObject: String, positionInList: Int, conceptId: String){
        val lang = getLanguage(jsonPathParser.parse(currentObject).read<List<String>>("$.*.*").toString());
        val resultWord : String = jsonValueParser.parse(currentObject).read<JSONArray>("$[0]$lang")[0] as String;

        when(resultWord.getSortCondition(positionInList,lang,conceptId)){
            SortConditions.SortWordAndLastWordIsNotSortWord -> fail("Error on $resultWord in position $positionInList on exact match: \n entry sorted after non-exact match")
            SortConditions.SortWordAndPrimaryLanguageAndLastLanguageIsPrimaryAndIsDuplicate -> fail("Error on $resultWord in position $positionInList primary language exact match:\n entry is duplicate")
            SortConditions.SortWordAndPrimaryLanguageAndLastLanguageIsNotPrimary -> fail("Error on $resultWord in position $positionInList primary language exact match: \n entry sorted after secondary language")
            SortConditions.SortWordAndSecondaryLanguageAndIsDuplicate -> fail("Error on $resultWord in position $positionInList secondary language exact match: \n entry is duplicate")
            SortConditions.NotSortWordAndIsDuplicate -> fail("Error on $resultWord in position $positionInList secondary match: entry is duplicate")
            SortConditions.SortWordAndCorrectlySorted -> {
                lastLangPath = notNullLang(lang)
                lastExactMatch += 1
                conceptIds.add(conceptId)
            }
            SortConditions.NotSortWordAndNotDuplicate -> {
                conceptIds.add(conceptId)
            }
        }
    }

    private fun String.getSortCondition(listPosition: Int, lang: String?, conceptId: String): SortConditions {
        return when {
            this == sortWord && lastExactMatch +1 != listPosition ->
                SortConditions.SortWordAndLastWordIsNotSortWord
            this == sortWord && lang == primaryLanguagePath && !(lastLangPath == "" || lastLangPath == primaryLanguagePath) ->
                SortConditions.SortWordAndPrimaryLanguageAndLastLanguageIsNotPrimary
            this == sortWord && lang == primaryLanguagePath && (lastLangPath == "" || lastLangPath == primaryLanguagePath) && conceptIds.contains(conceptId) ->
                SortConditions.SortWordAndPrimaryLanguageAndLastLanguageIsPrimaryAndIsDuplicate
            this == sortWord && lang == primaryLanguagePath && (lastLangPath == "" || lastLangPath == primaryLanguagePath) && !conceptIds.contains(conceptId) ->
                SortConditions.SortWordAndCorrectlySorted
            this == sortWord && lang != primaryLanguagePath && conceptIds.contains(conceptId) ->
                SortConditions.SortWordAndSecondaryLanguageAndIsDuplicate
            this != sortWord && conceptIds.contains(this) ->
                SortConditions.NotSortWordAndIsDuplicate
            else -> SortConditions.NotSortWordAndNotDuplicate
        }
    }

    fun getPathsForField(jsonPath : String): MutableList<String> {
        val paths = mutableListOf<String>();
        paths.addAll(pathParser.read<List<String>>(jsonPath))
        return paths;
    }
}

private fun notNullLang(lang: String?) : String = lang ?: ""

private enum class SortConditions{
    SortWordAndCorrectlySorted,
    NotSortWordAndNotDuplicate,
    SortWordAndLastWordIsNotSortWord,
    SortWordAndPrimaryLanguageAndLastLanguageIsNotPrimary,
    SortWordAndPrimaryLanguageAndLastLanguageIsPrimaryAndIsDuplicate,
    SortWordAndSecondaryLanguageAndIsDuplicate,
    NotSortWordAndIsDuplicate,
}

private fun  getLanguage(currentPath: String): String? {
    allLanguages.forEach { if(currentPath.contains(it)) return it }
    return null
}
