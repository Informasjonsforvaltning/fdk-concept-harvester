package no.dcat.client.publishercat;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import no.dcat.shared.Publisher;
import no.dcat.shared.PublisherFromOrganizationCatalog;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PublisherCatClient {
    @Value("${application.organizationCatalogueUrl}")
    private String organizationCatalogueUrl;

    public Publisher getByOrgNr(String orgNr) {
        RestTemplate restTemplate = new RestTemplate();

        String resourceUrl = getOrganizationCatalogueUrl() + "/organizations/"+orgNr;
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> entity = new HttpEntity<String>(headers);

        ResponseEntity<PublisherFromOrganizationCatalog> respEntity = restTemplate.exchange(resourceUrl, HttpMethod.GET, entity, PublisherFromOrganizationCatalog.class);
        return new Publisher(respEntity.getBody());
    }
}

