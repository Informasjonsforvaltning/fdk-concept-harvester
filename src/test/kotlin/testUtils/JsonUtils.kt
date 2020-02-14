package testUtils

import com.jayway.jsonpath.*
import net.minidev.json.JSONArray

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
                        val pathParser: DocumentContext)
{
    var exactMatchesId = mutableListOf<String>()
    var lastExactMatch = -1
    var lastPath = ""

    fun isLessRelevant(currentPath: String, currentObject: String, positionInList: Int, conceptId: String): Boolean {
        var correctlySorted = false
        val lang =
                getLanguage(jsonPathParser.parse(currentObject).read<List<String>>("$.*.*").toString());
        val value = jsonValueParser.parse(currentObject).read<JSONArray>("$[0]$lang")[0]

                if(value == sortWord) {
            //if current hit is an exact match, last match should be an exact match
            if (positionInList == lastExactMatch + 1) {
                lastExactMatch += 1
                //if the current hit is in primaryLanguage, last hit should also be in primaryLanguage
                if(lang == primaryLanguagePath){
                    if(lastPath == "" || lastPath == primaryLanguagePath) {
                        correctlySorted = true
                    }

                } else {
                    //If the concept has already been listed in primary language, it should not be listed on secondary languages
                    if(!exactMatchesId.contains(conceptId)){
                        correctlySorted = true
                    }
                }
            }
        } else {
            //TODO: better search specification:
            correctlySorted = true
        }

        return correctlySorted
    }

    private fun  getLanguage(currentPath: String): String? {
        var langPath : String? = null
        val langKeys = listOf<String>("en","nn","nb","no")

            for (key in langKeys){
                if(currentPath.contains(key)) {
                    langPath = key
                }
            }

        return langPath
    }

    fun getPathsForField(jsonPath : String, field : String): MutableList<String> {
        val paths = mutableListOf<String>();
        paths.addAll(pathParser.read<List<String>>(jsonPath))
        return paths;
    }
}
