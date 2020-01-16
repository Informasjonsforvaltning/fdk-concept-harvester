package no.ccat.service;

import no.ccat.dto.HarvestDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

@Service
public class HarvestAdminClient {
    private static final Logger logger = LoggerFactory.getLogger(HarvestAdminClient.class);

    private final RestTemplate restTemplate;
    private HttpHeaders defaultHeaders;

    @Value("${application.harvestAdminRootUrl}")
    private String apiHost;

    public HarvestAdminClient() {
        this.restTemplate = new RestTemplate();

        this.defaultHeaders = new HttpHeaders();
        defaultHeaders.setAccept(singletonList(MediaType.APPLICATION_JSON));
        defaultHeaders.setContentType(MediaType.APPLICATION_JSON);
    }

    List<HarvestDataSource> getDataSources() {
        return this.getDataSources(new LinkedMultiValueMap<>());
    }

    List<HarvestDataSource> getDataSources(MultiValueMap<String, String> queryParams) {
        String url = format("%s/datasources", this.apiHost);
        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl(url).queryParams(queryParams);

        try {
            ResponseEntity<List<HarvestDataSource>> response = restTemplate.exchange(
                    uriBuilder.toUriString(),
                    HttpMethod.GET,
                    new HttpEntity(defaultHeaders),
                    new ParameterizedTypeReference<List<HarvestDataSource>>() {});

            return response.hasBody() ? response.getBody() : emptyList();

        } catch (HttpClientErrorException e) {
            logger.error(String.format("Error fetching harvest urls from GET / %s. %s (%d)",
                    uriBuilder.toUriString(), e.getStatusText(), e.getStatusCode().value()));
        } catch (RestClientException e) {
            logger.error(e.getMessage());
        }

        return emptyList();
    }
}
