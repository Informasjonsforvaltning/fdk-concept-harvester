package no.ccat.service;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import no.ccat.common.model.ConceptDenormalized;
import no.ccat.dto.HarvestDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.util.LinkedMultiValueMap;

/*
    Fetch concepts and insert or update them in the search index.
 */
@Service
@RequiredArgsConstructor
public class ConceptHarvester {

    private static final Logger logger = LoggerFactory.getLogger(ConceptHarvester.class);

    private final Environment env;
    private final ConceptDenormalizedRepository conceptDenormalizedRepository;
    private final RDFToModelTransformer rdfToModelTransformer;
    private final HarvestAdminClient harvestAdminClient;
    private final ApplicationContext context;

    @Async
    @EventListener(ApplicationReadyEvent.class)
    void harvestOnce() {
        LinkedMultiValueMap<String,String> params= new LinkedMultiValueMap<>();
        params.add("dataType","concept");
        logger.info("Harvest of Concepts start");
        this.harvestAdminClient.getDataSources(params).forEach(dataSource -> harvestFromSingleURLSource(dataSource, false));
        logger.info("Harvest of Concepts complete");
        updateSearch();
    }

    void harvestFromSingleURLSource(HarvestDataSource dataSource, Boolean single) {
        Reader reader;

        String theEntireDocument = null;

        if (localHarvest()) {
            theEntireDocument = readURLFully(dataSource.getUrl(),"*/*");

        } else {
            theEntireDocument = readURLFully(dataSource.getUrl(), dataSource.getAcceptHeaderValue());
        }

        reader = new StringReader(theEntireDocument);

        if (reader == null) return;

        List<ConceptDenormalized> concepts = rdfToModelTransformer.getConceptsFromStream(reader);

        logger.info("Harvested {} concepts from publisher {} at Uri {} ", concepts.size(), dataSource.getPublisherId(), dataSource.getUrl());

        concepts.forEach(conceptDenormalizedRepository::save);

        if(single){
            updateSearch();
        }
    }

    private String readFileFully(String fileURI) {
        try {
            return new String(Files.readAllBytes(Paths.get(fileURI)));
        } catch (IOException ie) {
            logger.warn("File load failed for File URI " + fileURI);
            return null;
        }
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

    private boolean localHarvest(){
        boolean local = false;

        for (String profile :  env.getActiveProfiles()){
            if(profile.equals("dev-with-harvester") ){
                logger.info("Starting concept harvest from local file");
                local = true;
            }
        }
        return local;
    }

    private void updateSearch() {
        ObjectNode payload = JsonNodeFactory.instance.objectNode();

        AmqpTemplate rabbitTemplate = (AmqpTemplate)context.getBean("jsonRabbitTemplate");
        payload.put("updatesearch", "concepts");

        try {
            rabbitTemplate.convertAndSend("harvester.UpdateSearchTrigger", payload);
            logger.info("Successfully sent harvest message for publisher {}", payload);
        } catch (AmqpException e) {
            logger.error("Failed to send harvest message for publisher {}", payload, e);
        }
    }
}
