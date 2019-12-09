package no.ccat.service;

import no.ccat.model.ConceptDenormalized;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.List;

@RepositoryRestResource(itemResourceRel = "concept", collectionResourceRel = "concepts", exported = false)
public interface ConceptDenormalizedRepository
    extends ElasticsearchRepository<ConceptDenormalized, String> {

    @Query("{\"bool\":{\"must\":[{\"query_string\":{\"default_field\":\"identifier\",\"query\":\"\\\"?0\\\"\"}}]}}")
    ConceptDenormalized findByIdentifier(String identifier);
}
