package no.ccat.service

import mbuhot.eskotlin.query.compound.bool
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


    fun buildDocumentSearch(queryParams: QueryParams): QueryBuilder? {

        if (queryParams.isEmpty()){
            return MatchAllQueryBuilder()
        } else {
           return buildQueryString(queryParams)

        }
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

    private fun buildQueryString(queryParams: QueryParams) : QueryBuilder? {
        val builder : QueryBuilder;

        if(!queryParams.isEmptySearch()){
            builder = buildWithSearchString(
                    queryParams.queryString,
                    queryParams.lang
            )
        } else {
            builder =
                    match_all {  }
        }

        return builder;
    }

    private fun buildWithSearchString(searchString: String, preferredLanguage: String): DisMaxQueryBuilder{
        val langProperties =
                if (preferredLanguage == "") LanguageProperties()
                else LanguageProperties(preferredLanguage)

        val  titleBoost = buildMatchPhrasePrefixBoost(searchString,langProperties);

        val queryList = mutableListOf<QueryBuilder>(
                buildExactMatchScoreBoost(searchString, langProperties),
                QueryBuilders.simpleQueryStringQuery("$searchString $searchString*"))
        queryList.addAll(titleBoost)

        val query =
                dis_max {
                    queries = queryList
                }

        return query
    }
}