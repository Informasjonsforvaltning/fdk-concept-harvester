package no.ccat.controller;

import lombok.RequiredArgsConstructor;
import no.ccat.service.ConceptDenormalizedRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ApplicationStatusController {
    private static final Logger logger = LoggerFactory.getLogger(ApplicationStatusController.class);

    private final ConceptDenormalizedRepository conceptDenormalizedRepository;

    @GetMapping("/ping")
    public ResponseEntity<Void> ping() {
        return ResponseEntity.ok().build();
    }

    @GetMapping("/ready")
    public ResponseEntity<Void> ready() {
        try {
            conceptDenormalizedRepository.count();
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
    }


    @GetMapping("/count")
    public ResponseEntity<Long> count() {
        try {
            return ResponseEntity.ok(conceptDenormalizedRepository.count());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
    }
}
