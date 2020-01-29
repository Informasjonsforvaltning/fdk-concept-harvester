package no.ccat.dto

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import java.util.*

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder("id", "dataSourceType", "url", "publisherId", "description", "acceptHeaderValue")
class HarvestDataSource {
    @JsonProperty("id")
    val id: String? = null
    @JsonProperty("dataSourceType")
    val dataSourceType: String? = null
    @JsonProperty("url")
    val url: String? = null
    @JsonProperty("publisherId")
    val publisherId: String? = null
    @JsonProperty("description")
    val description: String? = null
    @JsonProperty("acceptHeaderValue")
    val acceptHeaderValue: String? = null
    @JsonIgnore
    val additionalProperties: Map<String, Any> = HashMap()
}