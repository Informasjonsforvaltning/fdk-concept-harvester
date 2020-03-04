package no.ccat.service

import mbuhot.eskotlin.query.compound.bool
import mbuhot.eskotlin.query.compound.dis_max
import mbuhot.eskotlin.query.fulltext.match
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

            when(queryParams.queryType) {
                QueryType.prefLabelSearch -> buildPrefLabelSearch(queryParams)
                QueryType.prefLabelSearcgWithOrgPath -> buildPrefLabelSearchWithOrgPath(queryParams)
                QueryType.queryStringSearch -> buildDocumentSearch(queryParams)
                QueryType.queryStringSearchWithOrgPath -> buildDocumentSearchWithOrgPath(queryParams)
                QueryType.urisSearch -> buildUrisSearchQuery(queryParams.uris!!)
                QueryType.identifiersSearch -> buildIdentifiersSearchQuery(queryParams.identifiers!!)
                QueryType.orgPathOnlySearch -> buildOrhPathOnlySearch(queryParams.orgPath)
                else -> match_all {}
            }

    private fun buildOrhPathOnlySearch(orgPath: String): QueryBuilder? =
              match {
                  "publisher.orgPath" {
                      query = orgPath
                      analyzer = "keyword"
                      operator= "AND"
                      minimum_should_match = "100%"
                  }
              }

    private fun buildIdentifiersSearchQuery(identifiers: Set<String>): QueryBuilder? =
            bool {
                must =  listOf (
                        bool {
                            should = buildIdentifierMatchQueries(identifiers)
                        }
                )
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

    private fun buildUrisSearchQuery(uris: Set<String>): QueryBuilder? =
            bool {
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
            if (!queryParams.isEmptySearchQuery()) {
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
