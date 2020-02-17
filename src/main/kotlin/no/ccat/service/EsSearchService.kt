package no.ccat.service

import mbuhot.eskotlin.query.compound.dis_max
import org.springframework.stereotype.Service
import mbuhot.eskotlin.query.term.match_all
import no.ccat.utils.*
import org.elasticsearch.index.query.*

@Service
class EsSearchService {

    fun buildSearch(queryParams: QueryParams): QueryBuilder? =
            if(queryParams.isPrefLabelSearch()) {
                buildPrefLabelSearch(queryParams)
            } else {
                buildDocumentSearch(queryParams)
            }

    private fun buildDocumentSearch(queryParams: QueryParams): QueryBuilder? =
            if (queryParams.isEmpty()){
                MatchAllQueryBuilder()
            } else {
                buildQueryString(queryParams)
            }

    private fun buildPrefLabelSearch(params: QueryParams): QueryBuilder{
        val langProperties = LanguageProperties(params.lang)
        val normalizedExactScore = buildExactMatchScoreBoost(params.prefLabel, langProperties, true)
        val matchPrefixPhrase = buildMatchPhrasePrefixBoost(params.prefLabel,langProperties)
        val disMaxQueries = mutableListOf<QueryBuilder>(
                normalizedExactScore
        )
        disMaxQueries.addAll(matchPrefixPhrase)

        return dis_max {
            queries= disMaxQueries
        }
    }

    private fun buildQueryString(queryParams: QueryParams) : QueryBuilder? =
            if(!queryParams.isEmptySearch()){
                buildWithSearchString(
                        queryParams.queryString,
                        queryParams.lang
                )
            } else {
                match_all {  }
            }

    private fun buildWithSearchString(searchString: String, preferredLanguage: String): DisMaxQueryBuilder{
        val langProperties = LanguageProperties(preferredLanguage)
        val prefLabelBoost = buildMatchPhrasePrefixBoost(searchString,langProperties);
        val queryList = mutableListOf<QueryBuilder>(
                buildExactMatchScoreBoost(searchString, langProperties),
                QueryBuilders.simpleQueryStringQuery("$searchString $searchString*"))
        queryList.addAll(prefLabelBoost)

        return dis_max {
                    queries = queryList
                }
    }
}