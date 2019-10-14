package no.ccat.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

@EqualsAndHashCode(callSuper = false)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
public class Concept extends HarvestTriggerMessage {
    @JsonProperty("publisherId")
    private String publisherId;
    @JsonProperty("catalogueId")
    private String catalogueId;
    @JsonProperty("conceptId")
    private String conceptId;

    @Override
    public MultiValueMap<String, String> queryParams() {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("publisherId", this.publisherId);
        params.add("catalogueId", this.catalogueId);
        params.add("catalogueId", this.conceptId);
        return params;
    }
}
