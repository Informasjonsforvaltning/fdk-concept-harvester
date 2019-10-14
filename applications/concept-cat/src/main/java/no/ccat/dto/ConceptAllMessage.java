package no.ccat.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.util.MultiValueMap;

@EqualsAndHashCode(callSuper = false)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
public class ConceptAllMessage extends HarvestTriggerMessage {
    @Override
    public MultiValueMap<String, String> queryParams() {
        return super.queryParams();
    }
}
