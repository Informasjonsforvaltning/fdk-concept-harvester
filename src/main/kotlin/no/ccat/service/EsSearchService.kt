package no.ccat.service

import mbuhot.eskotlin.query.compound.bool
import org.springframework.stereotype.Service
import mbuhot.eskotlin.query.term.match_all
import no.ccat.utils.*
import org.elasticsearch.index.query.BoolQueryBuilder
import org.elasticsearch.index.query.MatchAllQueryBuilder
import org.elasticsearch.index.query.QueryBuilder
import org.elasticsearch.index.query.QueryBuilders
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder

@Service
class EsSearchService {

    fun buildSearch(queryParams: QueryParams): QueryBuilder? =
         if(queryParams.isPrefLabelSearch()) {
            buildPrefLabelSearch()
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

    fun buildPrefLabelSearch(): MatchAllQueryBuilder {
        return MatchAllQueryBuilder()
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




    fun buildWithSearchString(searchString: String, preferredLanguage: String): BoolQueryBuilder {
        val langProperties =
                if (preferredLanguage == "") LanguageProperties()
                else LanguageProperties(preferredLanguage)

        val  titleBoost = buildMatchInTitleBoost(searchString,langProperties);

        val queryList = mutableListOf<QueryBuilder>(
                buildConstantScoreForExactMatch(searchString),
                QueryBuilders.simpleQueryStringQuery("$searchString $searchString*"))
        queryList.addAll(titleBoost)

        val query = bool {
            must {
                bool {
                    should = queryList
                }
            }
        }
        return query
    }
}