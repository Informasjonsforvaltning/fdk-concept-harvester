package no.ccat.service;

import lombok.RequiredArgsConstructor;
import no.ccat.common.model.ConceptDenormalized;
import no.ccat.dto.HarvestDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

/*
    Fetch concepts and insert or update them in the search index.
 */
@Service
@RequiredArgsConstructor
public class ConceptHarvester {
    private static final Logger logger = LoggerFactory.getLogger(ConceptHarvester.class);

    private final ConceptDenormalizedRepository conceptDenormalizedRepository;
    private final RDFToModelTransformer rdfToModelTransformer;
    private final HarvestAdminClient harvestAdminClient;

    private boolean isRunningForDeveloperLocally = false;

    @PostConstruct
    private void harvestOnce() {
        logger.info("Harvest of Concepts start");
        this.harvestAdminClient.getDataSources().forEach(this::harvestFromSingleURLSource);
        logger.info("Harvest of Concepts complete");
    }

    void harvestFromSingleURLSource(HarvestDataSource dataSource) {
        Reader reader;

        String theEntireDocument = null;

        if (isRunningForDeveloperLocally) {
            logger.info("Harvester isRunningForDeveloperLocally==true");
            theEntireDocument = readFileFully("c:\\tmp\\localConceptsFile.txt");
        } else {
            theEntireDocument = readURLFully(dataSource.getUrl(), dataSource.getAcceptHeaderValue());
        }

        reader = new StringReader(theEntireDocument);

        if (reader == null) return;

        List<ConceptDenormalized> concepts = rdfToModelTransformer.getConceptsFromStream(reader);

        logger.info("Harvested {} concepts from publisher {} at Uri {} ", concepts.size(), dataSource.getPublisherId(), dataSource.getUrl());

        concepts.forEach(conceptDenormalizedRepository::save);
    }

    private String readFileFully(String fileURI) {
        try {
            return new String(Files.readAllBytes(Paths.get(fileURI)));
        } catch (IOException ie) {
            logger.warn("File load failed for File URI " + fileURI);
        }
        return null;
    }

    private String readURLFully(String harvestSourceUri, String acceptHeader) {
        try {
            URL url = new URL(harvestSourceUri);
            logger.info("Start harvest from url: {}", url);
            URLConnection urlConnection = url.openConnection();
            urlConnection.setRequestProperty("Accept", acceptHeader);

            InputStream inputStream = urlConnection.getInputStream();

            java.util.Scanner s = new java.util.Scanner(inputStream).useDelimiter("\\A");
            String someString = s.hasNext() ? s.next() : "";
            return someString;

        } catch (IOException e) {
            logger.warn("Downloading concepts from url failed: {} Exception {}", harvestSourceUri, e.toString());
            logger.trace("Got exception when trying to harvest from url {} {}", harvestSourceUri, e.getStackTrace());
        }
        return "";
    }
}
