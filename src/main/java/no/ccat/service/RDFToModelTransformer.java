package no.ccat.service;

import no.ccat.SKOSNO;
import no.ccat.common.model.*;
import no.dcat.shared.HarvestMetadata;
import no.dcat.shared.HarvestMetadataUtil;
import no.dcat.shared.Publisher;
import no.dcat.shared.Schema;
import org.apache.jena.rdf.model.*;
import org.apache.jena.vocabulary.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.Reader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/*
    Transform RDF/Turtle into our domain model (ConceptDenormalized and friends)
*/
@Service
public class RDFToModelTransformer {

    public static final String defaultLanguage = "nb";
    private static final Logger logger = LoggerFactory.getLogger(RDFToModelTransformer.class);
    @Value("${application.apiRootExternalURL}")
    public String externalApiRoot;
    @Value("${application.conceptsPath}")
    public String conceptsPath;

    private ConceptBuilderService conceptBuilderService;
    private ConceptDenormalizedRepository conceptDenormalizedRepository;

    @Autowired
    public RDFToModelTransformer(ConceptBuilderService conceptBuilderService, ConceptDenormalizedRepository conceptDenormalizedRepository) {
        this.conceptBuilderService = conceptBuilderService;
        this.conceptDenormalizedRepository = conceptDenormalizedRepository;
    }

    public static Definition extractDefinition(Resource resource) {
        Definition definition = new Definition();
        definition.setText(new HashMap());
        definition.setRemark(new HashMap<>());
        Source source = new Source();
        definition.setSource(source);

        List<Resource> betydningsbeskivelses = getNamedSubPropertiesAsListOfResources(resource, SKOSNO.betydningsbeskrivelse);

        for (Resource betydningsbeskrivelse : betydningsbeskivelses) {
            definition.setRange(extractTextAndUri(betydningsbeskrivelse, SKOSNO.omfang));

            List<TextAndURI> sources = extractSources(definition, betydningsbeskrivelse);
            definition.setSources(sources);

            //We may need to merge the different language strings from the different betydningsbeskrivelses
            Map<String, String> definitionAsLanguageLiteral = extractLanguageLiteralFromResource(betydningsbeskrivelse, RDFS.label);
            if (definitionAsLanguageLiteral != null) {
                definition.getText().putAll(definitionAsLanguageLiteral);
            }

            Map<String, String> noteAsLanguageLiteral = extractLanguageLiteralFromResource(betydningsbeskrivelse, SKOS.scopeNote);
            if (noteAsLanguageLiteral != null) {
                definition.getRemark().putAll(noteAsLanguageLiteral);
            }

            Map<String, String> sourceAsLanguageLiteral = extractLanguageRDFSLabelFromLabel(betydningsbeskrivelse, DCTerms.source);
            if (sourceAsLanguageLiteral != null) {
                if (source.getPrefLabel() == null) {
                    source.setPrefLabel(new HashMap());
                }

                definition.getSource().getPrefLabel().putAll(sourceAsLanguageLiteral);
            }
        }
        return definition;
    }

    public static List<TextAndURI> extractSources(Definition definition, Resource resource) {
        //Sources are implemented as an array of rdfs:label & rdfs:seeAlso in a dct:source

        List<TextAndURI> results = new ArrayList<>();

        Statement sourceStatement = resource.getProperty(DCTerms.source);
        if (sourceStatement != null) {
            results.addAll(directlyExtractSources(sourceStatement.getResource()));
        }

        Statement forholdTilKildeStmt = resource.getProperty(SKOSNO.forholdTilKilde);
        if (forholdTilKildeStmt != null) {
            definition.setSourceRelationship(forholdTilKildeStmt.getObject().asResource().getLocalName());
            logger.debug(forholdTilKildeStmt.toString());
        }

        return results;
    }

    public static List<TextAndURI> directlyExtractSources(Resource resource) {
        List<TextAndURI> sources = new ArrayList<>();

        StmtIterator labelIterator = resource.listProperties(RDFS.label);
        StmtIterator seeAlsoIterator = resource.listProperties(RDFS.seeAlso);

        while (labelIterator.hasNext() || seeAlsoIterator.hasNext()) {
            TextAndURI textAndURI = new TextAndURI();

            if (labelIterator.hasNext()) {
                textAndURI.setText(extractLiteralFromStatement(labelIterator.next()));
            }

            if (seeAlsoIterator.hasNext()) {
                Resource seeAlsoResource = seeAlsoIterator.next().getResource();
                textAndURI.setUri(seeAlsoResource.getURI().startsWith("file") ? seeAlsoResource.getLocalName() : seeAlsoResource.getURI());
            }

            sources.add(textAndURI);
        }

        return sources;
    }

    private static List<Map<String, String>> extractLanguageMapList(Resource resource, Property property) {
        return resource.
            listProperties(property)
            .toList()
            .stream()
            .map(RDFToModelTransformer::extractLanguageMap)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }


    protected static String extractPublisherOrgNrFromStmt(Resource publisherResource) {
        try {
            URL url = new URL(publisherResource.getURI());
            String[] parts = url.getPath().split("/");

            return (parts[parts.length - 1]);
        } catch (Exception e) {
            logger.error("Failed while trying to parse URI for publisher {}", publisherResource.getURI());
            return null;
        }
    }

    public static Map<String, String> extractLiteralFromStatement(Statement stmt) {
        Map<String, String> result = new HashMap<>();
        String lang = stmt.getLanguage();
        String value = stmt.getString();
        result.put(lang, value);
        return result;
    }

    public static Map<String, String> extractLanguageLiteralFromResource(Resource resource, Property property) {
        Map<String, String> map = new HashMap<>();

        StmtIterator iterator = resource.listProperties(property);

        while (iterator.hasNext()) {
            Statement statement = iterator.next();
            String language = statement.getLanguage();
            if (language == null || language.isEmpty()) {
                language = defaultLanguage;
            }
            if (statement.getString() != null && !statement.getString().isEmpty()) {
                map.put(language, statement.getString());
            }
        }

        if (map.keySet().size() > 0) {
            return map;
        }

        return null;
    }

    private static Map<String, String> extractLanguageMap(Statement statement) {
        Map<String, String> map = new HashMap<>();

        String language = statement.getLanguage();
        String string = statement.getString();

        if (language == null || language.isEmpty()) {
            language = defaultLanguage;
        }

        if (string != null && !string.isEmpty()) {
            map.put(language, string);
        }

        return map.keySet().size() > 0 ? map : null;
    }

    public static Map<String, String> extractLanguageRDFSLabelFromLabel(Resource resource, Property property) {
        Statement stmt = resource.getProperty(property);
        if (stmt == null) {
            return null;
        }
        RDFNode node = stmt.getObject();
        Resource subResource = node.asResource();

        return extractLanguageLiteralFromResource(subResource, RDFS.label);
    }


    public static Map<String, String> extractLanguageLiteralFromLabel(Resource resource, Property property) {
        Statement stmt = resource.getProperty(property);
        if (stmt == null) {
            return null;
        }
        RDFNode node = stmt.getObject();
        Resource subResource = node.asResource();

        return extractLanguageLiteralFromResource(subResource, SKOSXL.literalForm);
    }

    public static List<Resource> getNamedSubPropertiesAsListOfResources(Resource source, Property target) {
        List<Resource> resources = new ArrayList<>();
        StmtIterator iterator = source.listProperties(target);
        while (iterator.hasNext()) {
            Statement stmt = iterator.next();
            resources.add(stmt.getObject().asResource());
        }
        return resources;
    }

    public List<ConceptDenormalized> getConceptsFromStream(Reader reader) {
        try {
            final Model model = ModelFactory.createDefaultModel();
            model.read(reader, null, "TURTLE");//Base and lang is just untested dummy values
            return getConceptsFromModel(model);
        } catch (Exception e) {
            logger.info("Got error while reading model: " + e.getMessage());
            return Collections.<ConceptDenormalized>emptyList();
        }
    }

    public List<ConceptDenormalized> getConceptsFromModel(Model model) {

        List<ConceptDenormalized> concepts = new ArrayList<>();

        ResIterator conceptIterator = model.listResourcesWithProperty(RDF.type, SKOS.Concept);

        while (conceptIterator.hasNext()) {
            Resource conceptResource = conceptIterator.nextResource();
            try {
              ConceptDenormalized conceptDenormalized = extractConceptFromModel(conceptResource);
              concepts.add(conceptDenormalized);
            }
            catch (Exception e) {
              logger.error("Got exception for Elasticsearch: {}", conceptResource.getURI(), e);
            }
        }
        return concepts;
    }

    public ConceptDenormalized extractConceptFromModel(Resource conceptResource) {
        ConceptDenormalized concept = new ConceptDenormalized();

        // Checking whether the concept is already harvested:
        ConceptDenormalized existingConcept = conceptDenormalizedRepository.findByIdentifier(conceptResource.getURI());

        Date harvestDate = new Date();
        HarvestMetadata oldMetadata = null;

        // If concept is harvested earlier, we reuse the harvest-metadata:
        if (existingConcept != null) {
            oldMetadata = existingConcept.getHarvest();
        }
        HarvestMetadata harvest = HarvestMetadataUtil.createOrUpdate(oldMetadata, harvestDate, false);

        String id = existingConcept != null ? existingConcept.getId() : UUID.randomUUID().toString();

        concept.setId(id);

        concept.setHarvest(harvest);

        concept.setUri(buildLocalUri(id));//So that URI is actually addressable into our system.

        concept.setIdentifier(conceptResource.getURI());

        concept.setPublisher(extractPublisher(conceptResource, DCTerms.publisher));

        concept.setSubject(extractLanguageLiteralFromResource(conceptResource, DCTerms.subject));

        concept.setExample(extractLanguageLiteralFromResource(conceptResource, SKOS.example));

        concept.setApplication(extractLanguageMapList(conceptResource, SKOSNO.bruksområde));

        concept.setPrefLabel(extractLanguageLiteralFromLabel(conceptResource, SKOSXL.prefLabel));

        concept.setHiddenLabel(extractLabels(conceptResource, SKOSXL.hiddenLabel));

        concept.setAltLabel(extractLabels(conceptResource, SKOSXL.altLabel));

        concept.setDefinition(extractDefinition(conceptResource));

        concept.setContactPoint(extractContactPoint(conceptResource));

        concept.setValidFromIncluding(extractDateFromResource(conceptResource, Schema.startDate));

        concept.setValidToIncluding(extractDateFromResource(conceptResource, Schema.endDate));

        concept.setSeeAlso(extractSeeAlso(conceptResource));

        return concept;
    }

    private List<Map<String, String>> extractLabels(Resource resource, Property property) {
            return !resource.hasProperty(property) ? new ArrayList() : resource
                    .getProperty(property)
                    .getResource()
                    .listProperties(SKOSXL.literalForm)
                    .toList()
                    .stream()
                    .map(RDFToModelTransformer::extractLanguageMap)
                    .collect(Collectors.toList());
    }

    private static TextAndURI extractTextAndUri(Resource resource, Property property) {
        Resource omfangResource = resource.getPropertyResourceValue(property);
        if (omfangResource == null) {
            return null;
        }

        TextAndURI textURI = new TextAndURI();
        textURI.setText(extractLanguageLiteralFromResource(omfangResource, RDFS.label));

        Statement theURI = omfangResource.getProperty(RDFS.seeAlso);
        if (theURI != null) {
            textURI.setUri(getEntireURIFromStatement(theURI));
        }
        return textURI;
    }

    private ContactPoint extractContactPoint(Resource resource) {
        ContactPoint contactPoint = new ContactPoint();

        try {
            Statement propertyStmnt = resource.getProperty(DCAT.contactPoint);
            if (propertyStmnt == null) {
                return null;
            }

            Resource contactPointResource = propertyStmnt.getObject().asResource();

            Statement phoneStatement = contactPointResource.getProperty(VCARD4.hasTelephone);
            if (phoneStatement != null) {
                String parsedPhoneNumber = getOnlySchemeSpesificPartOfURIFromStatement(phoneStatement);
                contactPoint.setTelephone(parsedPhoneNumber);
            }
            Statement emailStatement = contactPointResource.getProperty(VCARD4.hasEmail);
            if (emailStatement != null) {
                String parsedEmailAddress = getOnlySchemeSpesificPartOfURIFromStatement(emailStatement);
                contactPoint.setEmail(parsedEmailAddress);
            }
        } catch (Exception e) {
            logger.warn("Error when extracting property {} from resource {}", DCAT.contactPoint, resource.getURI(), e);
        }

        return contactPoint;
    }

    private static String getEntireURIFromStatement(Statement statement) {
        return parseUriFromStatement(statement).toString();
    }

    private static String getOnlySchemeSpesificPartOfURIFromStatement(Statement statement) {
        URI statementUri = parseUriFromStatement(statement);
        return statementUri==null ? null : statementUri.getSchemeSpecificPart();
    }

    private static URI parseUriFromStatement(Statement statement) {
        if (statement.getObject().isResource() && statement.getResource().isURIResource()) {
            try {
                URI uri = new URI(statement.getResource().getURI());
                return uri;
            } catch (URISyntaxException use) {
                logger.error("Email URI not parsable :" + statement.getObject().toString());
            }
        }
        return null;
    }

    private String buildLocalUri(String id) {
        return externalApiRoot + conceptsPath + "/" + id;
    }

    public Publisher extractPublisher(Resource resource, Property property) {
        try {
            Statement propertyStmnt = resource.getProperty(property);
            if (propertyStmnt != null) {
                Resource publisherResource = resource.getModel().getResource(propertyStmnt.getObject().asResource().getURI());
                String orgNr = extractPublisherOrgNrFromStmt(publisherResource);
                return conceptBuilderService.lookupPublisher(orgNr);
            }
        } catch (Exception e) {
            logger.warn("Error when extracting property {} from resource {}", DCTerms.publisher, resource.getURI(), e);
        }

        return null;
    }

    private List<String> extractSeeAlso(Resource resource) {
        if (resource.hasProperty(RDFS.seeAlso)) {
            try {
                return resource
                        .listProperties(RDFS.seeAlso)
                        .toList()
                        .stream()
                        .map(Statement::getString)
                        .filter(item-> !item.trim().isEmpty())
                        .collect(Collectors.toList());
            } catch (Exception e) {
                logger.error("Error when extracting property {} from resource {}", RDFS.seeAlso, resource.getURI(), e);
            }
        }
        return Collections.emptyList();
    }

    private LocalDate extractDateFromResource(Resource resource, Property property) {
        if (resource.hasProperty(DCTerms.temporal)) {
            Resource temporalResource = resource.getProperty(DCTerms.temporal).getResource();
            if (temporalResource.hasProperty(property)) {
                return LocalDate.parse(temporalResource.getProperty(property).getString());
            }
        }
        return null;
    }
}
