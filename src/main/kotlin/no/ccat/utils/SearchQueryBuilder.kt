package no.ccat.utils

import mbuhot.eskotlin.query.compound.constant_score
import org.elasticsearch.index.query.QueryBuilder
import org.elasticsearch.index.query.QueryBuilders
import org.elasticsearch.index.query.QueryBuilders.scriptQuery
import org.elasticsearch.script.Script


fun buildMatchInTitleBoost(searchString: String, preferredLanguage : LanguageProperties): List<QueryBuilder> {
    val utils = LanguageUtils()
    val secondaryLanguages = utils.getSecondaryLanguage(preferredLanguage.key)

    return listOf<QueryBuilder>(
            buildMatchWithLanguageBoost(searchString, preferredLanguage, 3f),
            buildMatchWithLanguageBoost(searchString, secondaryLanguages[0], 2f),
            buildMatchWithLanguageBoost(searchString, secondaryLanguages[1], 2f)
    )
}

fun buildMatchWithLanguageBoost(searchString: String, lang: LanguageProperties, boost : Float) : QueryBuilder =
        QueryBuilders.
            simpleQueryStringQuery("$searchString $searchString*")
            .field("prefLabel.${lang.key}")
            .analyzer(lang.analyzer)
            .boost(boost)



/*TODO implement english
* 1. update index in db
* 2. add prefLabel.en to scriptquery
* */
fun buildConstantScoreForExactMatch(searchString: String): QueryBuilder {
    val filterScript  = scriptQuery(
             Script("(doc['prefLabel.nn'].contains('$searchString') && doc['prefLabel.nn']?.size() == 1) || (doc['prefLabel.nb'].contains('$searchString') && doc['prefLabel.nb']?.size() == 1)")
    );

    val score = constant_score {
        filter{ filterScript }
        boost = 20f
    }
    return score
}

