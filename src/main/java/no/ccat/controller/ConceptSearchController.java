package no.ccat.controller;

import com.fasterxml.jackson.annotation.JsonInclude;
import no.ccat.common.model.ConceptDenormalized;
import no.ccat.service.EsSearchService;
import no.ccat.utils.QueryParams;
import no.fdk.webutils.aggregation.ResponseUtil;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.aggregation.AggregatedPage;
import org.springframework.data.elasticsearch.core.query.FetchSourceFilter;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.elasticsearch.core.query.SourceFilter;
import org.springframework.data.web.PageableDefault;
import org.springframework.hateoas.PagedResources;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static no.ccat.controller.Common.MISSING;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

@CrossOrigin
@RestController
@RequestMapping(value = "/concepts")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ConceptSearchController {

    private static final Logger logger = LoggerFactory.getLogger(ConceptSearchController.class);
    private ElasticsearchTemplate elasticsearchTemplate;
    private EsSearchService esSearchService;

    @Autowired
    public ConceptSearchController(ElasticsearchTemplate elasticsearchTemplate,
                                   EsSearchService esSearchService) {
        this.elasticsearchTemplate = elasticsearchTemplate;
        this.esSearchService = esSearchService;
    }

    /**
     * @param queryString simple query to be performed on all text
     * @param orgPath limit results to organisation with path
     * @param prefLabel use instead of queryString for searches on preflabel only, will be disregarded if queryString is present
     * @param startPage first page of search (default is 0)
     * @param size amount of concepts (default is 10)
     * @param returnFields fields to return
     * @param aggregations add aggregation of value buckets to response
     * @param sortfield sortOn: poosible values???
     * @param sortdirection sortDirection: possible values??
     * @param pageable pageable object for return
     * @return
     */

    @RequestMapping(value = "", method = RequestMethod.GET, produces = "application/json")
    public PagedResources<ConceptDenormalized> search(

        @RequestParam(value = "q", defaultValue = "" , required = false)
         String queryString,

        @RequestParam(value = "orgPath", defaultValue = "", required = false)
        String orgPath,

        @RequestParam(value = "prefLabel", defaultValue = "", required = false)
        String prefLabel,

        @RequestParam(value = "lang", defaultValue = "", required = false)
        String lang,

        @RequestParam (value = "page", defaultValue = "", required = false)
        String startPage,

        @RequestParam (value = "size", defaultValue = "", required = false)
        String size,

        @RequestParam(value = "returnfields", defaultValue = "", required = false)
            String returnFields,

        @RequestParam(value = "aggregations", defaultValue = "false", required = false)
            String aggregations,

        @RequestParam(value = "sortfield", defaultValue = "", required = false)
            String sortfield,

        @RequestParam(value = "sortdirection", defaultValue = "", required = false)
            String sortdirection,

        @RequestParam(value = "uris", required = false)
            Set<String> uris,

        @RequestParam(value = "identifiers",required = false)
            Set<String> identifiers,

        @PageableDefault()
            Pageable pageable
    ) {

         QueryParams params = new QueryParams(
                 queryString,
                 orgPath,
                 lang,
                 prefLabel,
                 startPage,
                 size,
                 returnFields,
                 aggregations,
                 sortfield,
                 sortdirection,
                 uris,
                 identifiers
                 );

         logger.debug("/GET search concepts ", params.toString());

        QueryBuilder searchQuery = esSearchService.buildSearch(params);

        NativeSearchQuery finalQuery = new NativeSearchQueryBuilder()
            .withQuery(searchQuery)
            .withIndices("ccat").withTypes("concept")
            .withPageable(pageable)
            .withSearchType(SearchType.DFS_QUERY_THEN_FETCH)
            .build();

        if (isNotEmpty(aggregations)) {
            finalQuery = addAggregations(finalQuery, aggregations);
        }
        if (isNotEmpty(returnFields)) {
            SourceFilter sourceFilter = new FetchSourceFilter(returnFields.concat(",prefLabel").split(","), null);
            finalQuery.addSourceFilter(sourceFilter);
        }

        if ("modified".equals(sortfield)) {
            Sort.Direction sortOrder = sortdirection.toLowerCase().contains("asc".toLowerCase()) ? Sort.Direction.ASC : Sort.Direction.DESC;
            String sortProperty = "harvest.firstHarvested";
            finalQuery.addSort(new Sort(sortOrder, sortProperty));
        }

        AggregatedPage<ConceptDenormalized> aggregatedPage = elasticsearchTemplate.queryForPage(finalQuery, ConceptDenormalized.class);
        List<ConceptDenormalized> concepts = aggregatedPage.getContent();

        stripEmptyObjects(concepts);

        PagedResources.PageMetadata pageMetadata = new PagedResources.PageMetadata(
            pageable.getPageSize(),
            pageable.getPageNumber(),
            aggregatedPage.getTotalElements(),
            aggregatedPage.getTotalPages()
        );

        PagedResources<ConceptDenormalized> conceptResources = new PagedResources<>(concepts, pageMetadata);

        if (aggregatedPage.hasAggregations()) {
            return ResponseUtil.addAggregations(conceptResources, aggregatedPage);
        } else {
            return conceptResources;
        }
    }

    public NativeSearchQuery addAggregations(NativeSearchQuery searchQuery, String aggregationFields) {
        HashSet<String> selectedAggregationFields = new HashSet<>(Arrays.asList(aggregationFields.split(",")));

        if (selectedAggregationFields.contains("orgPath")) {
            searchQuery.addAggregation(AggregationBuilders
                .terms("orgPath")
                .field("publisher.orgPath")
                .missing(MISSING)
                .size(Integer.MAX_VALUE)
                .order(Terms.Order.count(false)));
        }
        if (selectedAggregationFields.contains("firstHarvested")) {
            searchQuery.addAggregation(ESQueryUtil.createTemporalAggregation("firstHarvested", "harvest.firstHarvested"));
        }

        if (selectedAggregationFields.contains("publisher")) {
            searchQuery.addAggregation(ESQueryUtil.createTermsAggregation("publisher", "publisher.id.keyword"));
        }
        return searchQuery;
    }

    private void stripEmptyObjects(List<ConceptDenormalized> concepts) {
        //In order for spring to not include Source or Remark when its parts are empty we need to null out the source object itself.
        for (ConceptDenormalized concept : concepts) {
            ConceptGetController.stripEmptyObject(concept);
        }
    }
}
