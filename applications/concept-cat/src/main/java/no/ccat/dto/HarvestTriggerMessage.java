package no.ccat.dto;

import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

public abstract class HarvestTriggerMessage {
    public MultiValueMap<String, String> queryParams() {
        return new LinkedMultiValueMap<>();
    }
}
