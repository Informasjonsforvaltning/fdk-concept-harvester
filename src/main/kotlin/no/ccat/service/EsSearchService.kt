package no.ccat.service

import mbuhot.eskotlin.query.compound.bool
import mbuhot.eskotlin.query.compound.dis_max
import mbuhot.eskotlin.query.term.match_all
import mbuhot.eskotlin.query.term.term
import no.ccat.utils.*
import org.elasticsearch.index.query.BoolQueryBuilder
import org.elasticsearch.index.query.DisMaxQueryBuilder
import org.elasticsearch.index.query.QueryBuilder
import org.elasticsearch.index.query.QueryBuilders
import org.springframework.stereotype.Service

@Service
class EsSearchService {

    fun buildSearch(queryParams: QueryParams): QueryBuilder? =
            when {
                queryParams.isPrefLabelSearch() && (queryParams.orgPath == "") && !queryParams.isUriSearch() -> buildPrefLabelSearch(queryParams)
                queryParams.isPrefLabelSearch() && (queryParams.orgPath != "") && !queryParams.isUriSearch() -> buildPrefLabelSearchWithOrgPath(queryParams)
                !queryParams.isPrefLabelSearch() && (queryParams.orgPath == "") && !queryParams.isUriSearch() -> buildDocumentSearch(queryParams)
                !queryParams.isPrefLabelSearch() && (queryParams.orgPath != "") && !queryParams.isUriSearch() -> buildDocumentSearchWithOrgPath(queryParams)
                queryParams.isUriSearch() && queryParams.uris != null -> buildUrisSearchQuery(queryParams.uris)
                else -> match_all {}
            }

    private fun buildDocumentSearch(queryParams: QueryParams): QueryBuilder? =
            if (queryParams.isEmpty()) {
                match_all { }

            } else {
                buildQueryString(queryParams)
            }

    private fun buildDocumentSearchWithOrgPath(queryParams: QueryParams): QueryBuilder? =
            if (queryParams.queryString == "") {
                buildOrgPathQuery(queryParams)
            } else {
                bool {
                    must = buildQueryString(queryParams)?.let {
                        kotlin.collections.listOf<QueryBuilder>(
                                buildOrgPathQuery(queryParams),
                                it
                        )
                    }
                }

            }

    private fun buildUrisSearchQuery(uris: Set<String>): QueryBuilder? = bool {
        must = listOf(
                bool {
                    should = buildUrisQuery(uris)
                }
        )
    }

    private fun buildPrefLabelSearchWithOrgPath(params: QueryParams): QueryBuilder {
        val langProperties = LanguageProperties(params.lang)
        val normalizedExactScore = buildExactMatchScoreBoost(params.prefLabel, langProperties, true)
        val matchPrefixPhrase = buildMatchPhrasePrefixBoost(params.prefLabel, langProperties)
        val disMaxQueries = mutableListOf<QueryBuilder>(
                normalizedExactScore
        )
        disMaxQueries.addAll(matchPrefixPhrase)

        return bool {
            must = listOf(
                    dis_max {
                        queries = disMaxQueries
                    },
                    buildOrgPathQuery(params)
            )
        }
    }


    private fun buildPrefLabelSearch(params: QueryParams): QueryBuilder {
        val langProperties = LanguageProperties(params.lang)
        val normalizedExactScore = buildExactMatchScoreBoost(params.prefLabel, langProperties, true)
        val matchPrefixPhrase = buildMatchPhrasePrefixBoost(params.prefLabel, langProperties)
        val disMaxQueries = mutableListOf<QueryBuilder>(
                normalizedExactScore
        )
        disMaxQueries.addAll(matchPrefixPhrase)

        return dis_max {
            queries = disMaxQueries
        }
    }


    private fun buildQueryString(queryParams: QueryParams): QueryBuilder? =
            if (!queryParams.isEmptySearch()) {
                buildWithSearchString(
                        queryParams.queryString,
                        queryParams.lang
                )
            } else {
                match_all { }
            }

    private fun buildWithSearchString(searchString: String, preferredLanguage: String): DisMaxQueryBuilder {
        val langProperties = LanguageProperties(preferredLanguage)
        val prefLabelBoost = buildMatchPhrasePrefixBoost(searchString, langProperties);
        val queryList = mutableListOf<QueryBuilder>(
                buildExactMatchScoreBoost(searchString, langProperties),
                QueryBuilders.simpleQueryStringQuery("$searchString $searchString*"))
        queryList.addAll(prefLabelBoost)

        return dis_max {
            queries = queryList
        }
    }
}


private fun BoolQueryBuilder.addOrgPathFilter(orgPath: String): BoolQueryBuilder {
    this.filter(term { "orgPath" to orgPath })
    return this
}
