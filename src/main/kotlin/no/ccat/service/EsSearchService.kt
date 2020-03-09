package no.ccat.service

import mbuhot.eskotlin.query.compound.bool
import mbuhot.eskotlin.query.compound.dis_max
import mbuhot.eskotlin.query.term.match_all
import no.ccat.utils.*
import org.elasticsearch.index.query.DisMaxQueryBuilder
import org.elasticsearch.index.query.QueryBuilder
import org.springframework.stereotype.Service

@Service
class EsSearchService {

    fun buildSearch(queryParams: QueryParams): QueryBuilder? =
        queryParams.sanitize().buildSearchFromParams()

    private fun QueryParams.buildSearchFromParams(): QueryBuilder? =
            when(queryType) {
                QueryType.prefLabelSearch -> buildPrefLabelSearch(this)
                QueryType.prefLabelSearcgWithOrgPath -> buildPrefLabelSearchWithOrgPath(this)
                QueryType.queryStringSearch -> buildEntireDocumentSearch(this)
                QueryType.queryStringSearchWithOrgPath -> buildDocumentSearchWithOrgPath(this)
                QueryType.urisSearch -> buildUrisSearchQuery(uris!!)
                QueryType.identifiersSearch -> buildIdentifiersSearchQuery(identifiers!!)
                QueryType.orgPathOnlySearch -> buildOrgPathQuery(this)
                else -> match_all {}
            }

    private fun buildIdentifiersSearchQuery(identifiers: Set<String>): QueryBuilder? =
            bool {
                must =  listOf (
                        bool {
                            should = buildIdentifierMatchQueries(identifiers)
                        }
                )
            }

    private fun buildEntireDocumentSearch(queryParams: QueryParams): QueryBuilder? =
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
        return bool {
            must = listOf(
                    dis_max {
                        queries = listOf(
                                buildExactMatchQuery(params.prefLabel, langProperties),
                                buildPrefixBoostQuery(params.prefLabel, langProperties),
                                buildStringInPrefLabelQuery(params.prefLabel,langProperties)
                        )
                    },
                    buildOrgPathQuery(params)
            )
        }
    }

    private fun buildPrefLabelSearch(params: QueryParams): QueryBuilder {
        val langProperties = LanguageProperties(params.lang)
        return dis_max {
                queries = listOf(
                        buildExactMatchQuery(params.prefLabel, langProperties),
                        buildPrefixBoostQuery(params.prefLabel, langProperties),
                        buildStringInPrefLabelQuery(params.prefLabel,langProperties)
                )
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
        val queryList = mutableListOf<QueryBuilder>(
                buildExactMatchQuery(searchString, langProperties),
                buildStringInPrefLabelQuery(searchString,langProperties),
                buildSimpleStringQuery(searchString),
                buildPrefixBoostQuery(searchString, langProperties)
        )
        return dis_max {
            queries = queryList
        }
    }
}
