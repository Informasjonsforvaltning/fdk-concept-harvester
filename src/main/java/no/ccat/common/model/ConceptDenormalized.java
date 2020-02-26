package no.ccat.common.model;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import no.dcat.shared.HarvestMetadata;
import no.dcat.shared.Publisher;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.hateoas.core.Relation;

@Data
@EqualsAndHashCode(callSuper = true)
@Relation(value = "concept", collectionRelation = "concepts")
@Document(indexName = "ccat", type = "concept")
public class ConceptDenormalized extends Concept {

    @ApiModelProperty("The publisher of the concept [dct:publisher]")
    private Publisher publisher;

    @ApiModelProperty("The source where the record was harvested from")
    private String harvestSourceUri;

    @ApiModelProperty("information about when the concept was first and last harvested by the system")
    private HarvestMetadata harvest;

}
