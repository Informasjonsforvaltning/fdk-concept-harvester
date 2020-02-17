package testUtils

import com.jayway.jsonpath.*
import net.minidev.json.JSONArray
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
        val lang =
                getLanguage(jsonPathParser.parse(currentObject).read<List<String>>("$.*.*").toString());

        val resultWord : String = jsonValueParser.parse(currentObject).read<JSONArray>("$[0]$lang")[0] as String;

        when(resultWord.getSortCondition(positionInList,lang,conceptId)){
            SortConditions.sortWordAndLastWordIsNotSortWord -> fail("Error on $resultWord in position $positionInList on exact match: \n entry sorted after non-exact match")
            SortConditions.sortWordAndPrimaryLanguageAndLastLanguageIsPrimaryAndIsDuplicate -> fail("Error on $resultWord in position $positionInList primary language exact match:\n entry is duplicate")
            SortConditions.sortWordAndPrimaryLanguageAndLastLanguageIsNotPrimary -> fail("Error on $resultWord in position $positionInList primary language exact match: \n entry sorted after secondary language")
            SortConditions.sortWordAndSecondaryLanguageAndIsDuplicate -> fail("Error on $resultWord in position $positionInList secondary language exact match: \n entry is duplicate")
            SortConditions.notSortWordAndIsDuplicate -> fail("Error on $resultWord in position $positionInList secondary match: entry is duplicate")
            SortConditions.sortWordAndcorrectlySorted -> {
                lastLangPath = notNullLang(lang)
                lastExactMatch += 1
                conceptIds.add(conceptId)
            }
            SortConditions.notSortWordAndCorrectlySorted -> {
                conceptIds.add(conceptId)
            }
        }
    }

    private fun String.getSortCondition(listPosition: Int, lang: String?, conceptId: String): SortConditions {
        return when {
            this == sortWord && lastExactMatch +1 != listPosition ->
                SortConditions.sortWordAndLastWordIsNotSortWord
            this == sortWord && lang == primaryLanguagePath && !(lastLangPath == "" || lastLangPath == primaryLanguagePath) ->
                SortConditions.sortWordAndPrimaryLanguageAndLastLanguageIsNotPrimary
            this == sortWord && lang == primaryLanguagePath && (lastLangPath == "" || lastLangPath == primaryLanguagePath) && conceptIds.contains(conceptId) ->
                SortConditions.sortWordAndPrimaryLanguageAndLastLanguageIsPrimaryAndIsDuplicate
            this == sortWord && lang == primaryLanguagePath && (lastLangPath == "" || lastLangPath == primaryLanguagePath) && !conceptIds.contains(conceptId) ->
                SortConditions.sortWordAndcorrectlySorted
            this == sortWord && lang != primaryLanguagePath && conceptIds.contains(conceptId) ->
                SortConditions.sortWordAndSecondaryLanguageAndIsDuplicate
            this != sortWord && conceptIds.contains(this) ->
                SortConditions.notSortWordAndIsDuplicate
            else -> SortConditions.notSortWordAndCorrectlySorted
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
    sortWordAndcorrectlySorted,
    notSortWordAndCorrectlySorted,
    sortWordAndLastWordIsNotSortWord,
    sortWordAndPrimaryLanguageAndLastLanguageIsNotPrimary,
    sortWordAndPrimaryLanguageAndLastLanguageIsPrimaryAndIsDuplicate,
    sortWordAndSecondaryLanguageAndIsDuplicate,
    notSortWordAndIsDuplicate,
}

private fun  getLanguage(currentPath: String): String? {
    var langPath : String? = null
    val langKeys = listOf<String>("en","nn","nb","no")
    langKeys.forEach {
        if(currentPath.contains(it)) langPath = it
    }
    return langPath
}