package no.ccat.utils

import mbuhot.eskotlin.query.compound.bool
import mbuhot.eskotlin.query.compound.constant_score
import mbuhot.eskotlin.query.fulltext.match
import mbuhot.eskotlin.query.fulltext.match_phrase_prefix
import org.elasticsearch.index.query.QueryBuilder
import org.elasticsearch.index.query.QueryBuilders.*
import org.elasticsearch.script.Script
import java.lang.StringBuilder


fun buildSearchStringInPrefLabelBoost(searchString: String, preferredLanguage : LanguageProperties): List<QueryBuilder> {
    val utils = LanguageUtils()
    val secondaryLanguages = utils.getSecondaryLanguage(preferredLanguage.key)

    val builders =  mutableListOf<QueryBuilder>(
            buildSearchStringLanguageBoost(searchString, preferredLanguage, "prefLabel",3f)
    )

    for(language in secondaryLanguages){
        builders.add(buildSearchStringLanguageBoost(searchString,language, "prefLabel",2f))
    }

    return builders;

}

fun buildMatchPhrasePrefixBoost(searchString: String, preferredLanguage : LanguageProperties): List<QueryBuilder> {
    val utils = LanguageUtils()
    val secondaryLanguages = utils.getSecondaryLanguage(preferredLanguage.key)

    val builders = mutableListOf<QueryBuilder>(
            buildPrefixMatchWithLanguageBoost(searchString, preferredLanguage, 2f)
    )

    for (language in secondaryLanguages){
        builders.add(buildPrefixMatchWithLanguageBoost(searchString,language, 1f))
    }

    return builders
}

fun buildSearchStringLanguageBoost(searchString: String, lang: LanguageProperties, boostField: String, boost : Float) : QueryBuilder =
            simpleQueryStringQuery("$searchString $searchString*")
            .field("$boostField.${lang.key}")
            .analyzer(lang.analyzer)
            .boost(boost)

fun buildPrefixMatchWithLanguageBoost(searchString: String, lang: LanguageProperties, boost : Float) : QueryBuilder =
    match_phrase_prefix {
        "prefLabel.${lang.key}"  {
            query = searchString
            max_expansions = 15

            }
    }.boost(boost)


/*TODO (!!!prod and staging!!)
* 1. update index in elasticsearch with nn, en and nb as datafields
 */
fun buildExactMatchScoreBoost(searchString: String, primaryLanguage: LanguageProperties, isPrefLabelSearch: Boolean = false): QueryBuilder {

    val searchSpan = getSearchSpan(isPrefLabelSearch,searchString)
            if (isPrefLabelSearch) { searchString.length + 4}
            else {searchString.length}

    val stringBuilder : StringBuilder = StringBuilder().append(getExactMatchString(primaryLanguage.key, searchString, searchSpan))
    val secondaryLanguage = primaryLanguage.secondaryKeys()

    for (i in secondaryLanguage.indices){
        stringBuilder
                .append(" || ")
                .append(getExactMatchString(secondaryLanguage[i], searchString, searchSpan))
    }

    val filterScript  = scriptQuery(
             Script(stringBuilder.toString())
    );
    val score = constant_score {
        filter{ filterScript }
        boost = 30f
    }
    return bool {
        must= listOf(score)
        should = listOf(match_phrase_prefix {  "prefLabel.${primaryLanguage.key}" to searchString})
    }
}

fun getExactMatchString(langKey: String, searchString: String, matchSpan: Int ) =
        "(doc['prefLabel.$langKey'][0].contains('$searchString') " +
        "&& doc['prefLabel.$langKey'].size() == 1 " +
        "&& doc['prefLabel.$langKey'][0].length() < $matchSpan)"


fun getSearchSpan(isPrefLabelSearch: Boolean, searchString:String): Int =
    if(isPrefLabelSearch){
        getPrefLabelSpan(searchString)
    } else {
        searchString.length + 1
    }


private fun getPrefLabelSpan(searchString: String) : Int =
        if(searchString.length < 6 ) {
            10
        } else {
            searchString.length + 3
        }
