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
public class ConceptPublisherMessage extends HarvestTriggerMessage {
    @JsonProperty("publisherId")
    private String publisherId;

    @Override
    public MultiValueMap<String, String> queryParams() {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("publisherId", this.publisherId);
        return params;
    }
}
