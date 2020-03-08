package no.ccat.utils

import mbuhot.eskotlin.query.compound.bool
import mbuhot.eskotlin.query.compound.constant_score
import mbuhot.eskotlin.query.fulltext.match
import mbuhot.eskotlin.query.fulltext.match_phrase_prefix
import mbuhot.eskotlin.query.fulltext.multi_match
import mbuhot.eskotlin.query.term.exists
import mbuhot.eskotlin.query.term.term
import org.elasticsearch.index.query.MultiMatchQueryBuilder
import org.elasticsearch.index.query.QueryBuilder
import org.elasticsearch.index.query.QueryBuilders.queryStringQuery
import org.elasticsearch.index.query.QueryBuilders.simpleQueryStringQuery
import org.elasticsearch.index.query.SimpleQueryStringBuilder

fun buildPrefixBoostQuery(searchString: String, preferredLanguage : LanguageProperties): MultiMatchQueryBuilder =
    multi_match {
        query = searchString
        fields = preferredLanguage.toPrefixFieldList()
        type = "phrase_prefix"
    }

fun buildExactMatchQuery(searchString: String, primaryLanguage: LanguageProperties): QueryBuilder {
    val constantScoreQuery = constant_score {
        filter{
            bool {
                should = allLanguages.map {
                    term {
                        "prefLabel.$it.raw" to searchString
                    }
                }
            }
        }
        boost = 30f
    }
    return bool {
        must= listOf(constantScoreQuery)
        should = listOf(
                match_phrase_prefix {
                    "prefLabel.${primaryLanguage.key}" to searchString
                }
        )
    }
}

fun buildStringInPrefLabelQuery(searchString: String, lang: LanguageProperties) =
        queryStringQuery("*${searchString.esSafe()}*")
                .fields(lang.toQueryStringFieldList())

fun buildSimpleStringQuery(searchString: String) : SimpleQueryStringBuilder =
        simpleQueryStringQuery("$searchString $searchString*")
                .boost(0.1F)


fun buildOrgPathQuery(queryParams: QueryParams) =
        if(queryParams.orgPath == "MISSING") {
            buildMissingOrgPathQuery()}
        else {
            match {
                "publisher.orgPath" {
                    query = queryParams.orgPath
                    analyzer = "keyword"
                    operator= "AND"
                    minimum_should_match = "100%"
                }
            }
        }

private fun buildMissingOrgPathQuery() =
        bool { must_not = listOf(exists { field = "publisher.orgPath" }) }

fun buildUrisQuery(uris: Set<String>) =
    uris.map {
        match {
            "uri" {
                query = it
                analyzer = "keyword"
                operator= "AND"
                minimum_should_match = "100%"
            }
        }
    }

fun buildIdentifierMatchQueries(uris: Set<String>) =
    uris.map {
        match {
            "identifier" {
                query = it
                analyzer = "keyword"
                operator= "AND"
                minimum_should_match = "100%"
            }
        }
    }