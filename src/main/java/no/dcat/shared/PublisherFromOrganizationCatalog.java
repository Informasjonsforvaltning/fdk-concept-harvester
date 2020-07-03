package no.dcat.shared;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.ToString;

import java.util.Map;

@Data
@ToString(includeFieldNames = false)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PublisherFromOrganizationCatalog {
    private String norwegianRegistry;
    private String id;
    private String name;
    private String orgPath;
    private Map<String, String> prefLabel;

    public PublisherFromOrganizationCatalog(String orgnr) {
        this.id = orgnr;
    }

    public PublisherFromOrganizationCatalog(){

    }
}
