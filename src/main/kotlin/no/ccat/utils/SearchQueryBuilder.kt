package no.ccat.utils

import mbuhot.eskotlin.query.compound.bool
import mbuhot.eskotlin.query.compound.constant_score
import mbuhot.eskotlin.query.fulltext.match_phrase_prefix
import org.elasticsearch.index.query.QueryBuilder
import org.elasticsearch.index.query.QueryBuilders.*
import org.elasticsearch.index.query.ScriptQueryBuilder
import org.elasticsearch.script.Script
import java.lang.StringBuilder


fun buildSearchStringInPrefLabelBoost(searchString: String, preferredLanguage : LanguageProperties): List<QueryBuilder> {
    val builders =  mutableListOf<QueryBuilder>(
            buildSearchStringLanguageBoost(searchString, preferredLanguage, "prefLabel",3f)
    )
    secondaryLanguages(preferredLanguage.key).forEach {
        builders.add(buildSearchStringLanguageBoost(searchString, it,"prefLabel",2f))
    }
    return builders;
}

fun buildMatchPhrasePrefixBoost(searchString: String, preferredLanguage : LanguageProperties): List<QueryBuilder> {
    val builders = mutableListOf<QueryBuilder>(
            buildPrefixMatchWithLanguageBoost(searchString, preferredLanguage, 2f)
    )
    secondaryLanguages(preferredLanguage.key).forEach{
        builders.add(buildPrefixMatchWithLanguageBoost(searchString,it, 1f))
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

fun buildExactMatchScoreBoost(searchString: String, primaryLanguage: LanguageProperties, isPrefLabelSearch: Boolean = false): QueryBuilder {
    val constantsScoreQuery = constant_score {
        filter{
            buildExactMatchScript(searchString,primaryLanguage,isPrefLabelSearch)
        }
        boost = 30f
    }
    return bool {
        must= listOf(constantsScoreQuery)
        should = listOf(match_phrase_prefix {  "prefLabel.${primaryLanguage.key}" to searchString})
    }
}

fun buildExactMatchScript (searchString: String, primaryLanguage: LanguageProperties, isPrefLabelSearch: Boolean): ScriptQueryBuilder {
    val searchSpan = getSearchSpan(isPrefLabelSearch,searchString)
    val stringBuilder : StringBuilder = StringBuilder().append(buildExactMatchString(primaryLanguage.key, searchString, searchSpan))
    val secondaryLanguage = primaryLanguage.secondaryKeys()
    secondaryLanguage.forEach {
        stringBuilder
                .append(" || ")
                .append(buildExactMatchString(it, searchString, searchSpan))
    }

    return scriptQuery(Script(stringBuilder.toString()))
}

fun buildExactMatchString(langKey: String, searchString: String, matchSpan: Int ) =
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
