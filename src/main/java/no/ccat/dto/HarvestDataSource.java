package no.ccat.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "id",
        "dataSourceType",
        "url",
        "publisherId",
        "description",
        "acceptHeaderValue",
        "dataType"
})
public class HarvestDataSource {
    @JsonProperty("id")
    private String id;
    @JsonProperty("dataSourceType")
    private String dataSourceType;
    @JsonProperty("url")
    private String url;
    @JsonProperty("publisherId")
    private String publisherId;
    @JsonProperty("description")
    private String description;
    @JsonProperty("acceptHeaderValue")
    private String acceptHeaderValue;
    @JsonProperty("dataType")
    private String dataType;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<>();
}