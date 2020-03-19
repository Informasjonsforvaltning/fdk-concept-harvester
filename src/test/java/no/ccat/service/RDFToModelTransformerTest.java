package no.ccat.service;

import no.ccat.common.model.ConceptDenormalized;
import no.ccat.common.model.TextAndURI;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.Tag;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.time.LocalDate;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

@Tag("unit")
public class RDFToModelTransformerTest {

    private RDFToModelTransformer transformer;
    private ConceptDenormalizedRepository conceptDenormalizedRepository;
    private ConceptBuilderService conceptBuilderService;
    private Environment environment;

    @Before
    public void setup() {

        conceptDenormalizedRepository = mock(ConceptDenormalizedRepository.class);
        conceptBuilderService = mock(ConceptBuilderService.class);
        environment = mock(Environment.class);

        transformer = new RDFToModelTransformer(conceptBuilderService, conceptDenormalizedRepository, environment);
    }

    @Test
    public void testSingleConcept() throws Throwable {
        Reader reader = new InputStreamReader(new ClassPathResource("difiConcept.turtle").getInputStream());

        List<ConceptDenormalized> concepts = transformer.getConceptsFromStream(reader);

        assertEquals("We should get 1 concept", 1, concepts.size());

        ConceptDenormalized testConcept = concepts.get(0);
        assertNotNull(testConcept.getExample());
        assertNotNull(testConcept.getDefinition());
        assertEquals(2, testConcept.getApplication().size());
        assertEquals("fotball",testConcept.getApplication().get(0).get("nb"));
        assertEquals("tennis",testConcept.getApplication().get(1).get("nb"));
        assertEquals("https://somewhere.com",testConcept.getDefinition().getRange().getUri());
        assertEquals("someomfangtekst",testConcept.getDefinition().getRange().getText().get("nb"));
    }

    @Test
    public void testSourceInConceptAndHarvestFromConceptCatalogue() throws Throwable {
        Reader reader = new InputStreamReader(new ClassPathResource("ConceptCatalogueHarvest.turtle").getInputStream());

        List<ConceptDenormalized> concepts = transformer.getConceptsFromStream(reader);

        assertEquals("We should get 2 concept", 2, concepts.size());
    }

    @Test
    public void testSourceInConceptAndHarvestFromDataBrregNo() throws Throwable {
        // curl -i -H "Accept: text/turtle" https://data.brreg.no/begrep/ > data.brreg.no_begrep.turtle
        Reader reader = new InputStreamReader(new ClassPathResource("dev.data/data.brreg.no_begrep.turtle").getInputStream());

        List<ConceptDenormalized> concepts = transformer.getConceptsFromStream(reader);

        assertEquals("We should get 181 concept", 181, concepts.size());
    }

    @Test
    public void testSourceUrlShouldHaveProtocolIfSet() throws Throwable {
        Reader reader = new InputStreamReader(new ClassPathResource("ConceptCatalogueHarvest.turtle").getInputStream());

        List<ConceptDenormalized> concepts = transformer.getConceptsFromStream(reader);

        ConceptDenormalized conceptOfInterest = concepts.get(1);

        List<TextAndURI> sources = conceptOfInterest.getDefinition().getSources();

        assertEquals("https://vg.no", sources.get(0).getUri());
        assertEquals("www.uib.no", sources.get(1).getUri());
        assertEquals("www.uio.no", sources.get(2).getUri());

    }

    @Test
    public void testConceptShouldHaveSeeAlsoReferencesIfSet() throws IOException {
        Reader reader = new InputStreamReader(new ClassPathResource("digdirConcept.turtle").getInputStream());

        List<ConceptDenormalized> concepts = transformer.getConceptsFromStream(reader);

        ConceptDenormalized conceptOfInterest = concepts.get(0);

        List<String> seeAlsoReferences = conceptOfInterest.getSeeAlso();

        assertFalse("Expect concept to have see also references", seeAlsoReferences.isEmpty());
    }

    @Test
    public void testConceptShouldHaveVadlidToIncludingAndFromIncludingDatesIfSet() throws IOException {
        Reader reader = new InputStreamReader(new ClassPathResource("digdirConcept.turtle").getInputStream());

        List<ConceptDenormalized> concepts = transformer.getConceptsFromStream(reader);

        ConceptDenormalized conceptOfInterest = concepts.get(0);

        LocalDate validFromIncluding = conceptOfInterest.getValidFromIncluding();
        LocalDate validToIncluding = conceptOfInterest.getValidToIncluding();

        assertNotNull("Expect concept to have valid from including date", validFromIncluding);
        assertNotNull("Expect concept to have valid to including date", validToIncluding);
    }
}
