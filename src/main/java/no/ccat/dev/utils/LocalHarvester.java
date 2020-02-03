package no.ccat.dev.utils;

import no.ccat.common.model.ConceptDenormalized;
import no.ccat.service.ConceptDenormalizedRepository;
import no.ccat.service.RDFToModelTransformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

@Service
public class LocalHarvester {

    private static final Logger logger = LoggerFactory.getLogger(LocalHarvester.class);

    @Autowired
    RDFToModelTransformer rdfToModelTransformer;

    @Autowired
    ConceptDenormalizedRepository conceptDenormalizedRepository;

    public void harvestFromSingleURLSource(String source) {
        Reader reader;

        if(source == null){
            source = "/Users/bbreg/Documents/concept-catalouge/fdk-concept-harvester/src/main/resources/dev.data/arkivverket_fdk.turtle";
        }
        String theEntireDocument = null;

        theEntireDocument = readFileFully(source);


        reader = new StringReader(theEntireDocument);

        if (reader == null) return;

        List<ConceptDenormalized> concepts = rdfToModelTransformer.getConceptsFromStream(reader);

        logger.info("Local data read complected, concepts collected:" + concepts.size());

        concepts.stream().forEach(concept -> {
            conceptDenormalizedRepository.save(concept);
        });

        logger.info("Local data harvest completed");
    }

    private String readFileFully(String fileURI) {
        try {
            return new String(Files.readAllBytes(Paths.get(fileURI)));
        } catch (IOException ie) {
            logger.warn("File load failed for File URI " + fileURI);
        }
        return null;
    }

}
