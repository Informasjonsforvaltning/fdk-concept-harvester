package no.fdk.fdk_concept_harvester.tbx

import no.fdk.fdk_concept_harvester.Application
import no.fdk.fdk_concept_harvester.adapter.OrganizationsAdapter
import org.apache.jena.ext.xerces.util.URI
import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.rdf.model.RDFNode
import org.apache.jena.rdf.model.Resource
import org.apache.jena.vocabulary.*
import org.slf4j.LoggerFactory
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import java.io.ByteArrayInputStream
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.xpath.XPath
import javax.xml.xpath.XPathConstants
import javax.xml.xpath.XPathFactory

private val logger = LoggerFactory.getLogger(Application::class.java)

private val SKOSNO_NS = "https://data.norge.no/vocabulary/skosno#"
private val SKOSNO_DEFINISJON = "Definisjon"
private val SKOSNO_DEFINISJON_PROPERTY = "definisjon"

fun parseTBXResponse(tbxContent: String, rdfSource: String?, orgAdapter: OrganizationsAdapter): Model? {
    val model = ModelFactory.createDefaultModel()
        .setPrefixes()

    try {
        // Parse TBX
        val doc = DocumentBuilderFactory
            .newInstance()
            .newDocumentBuilder()
            .parse(ByteArrayInputStream(tbxContent.toByteArray()))

        val xpath = XPathFactory.newInstance().newXPath()

        // reading tbxHeader
        val collectionResource = model.createResource(xpath.collectionUri(doc))
        val publisherResource = xpath.publisherUri(doc, orgAdapter)?.let { model.createResource(it) }

        model.add(model.createStatement(collectionResource, RDF.type, SKOS.Collection))
        model.add(
            model.createStatement(collectionResource, RDFS.label,
            model.createLiteral(xpath.collectionName(doc), "en")))
        model.addPublisher(collectionResource, publisherResource)

        // reading conceptEntry
        xpath.conceptEntries(doc)
            .forEach { addConceptEntryNodeToModel(it as Element, doc, collectionResource, publisherResource, model) }

    } catch (ex: Exception) {
        logger.error("Parse from $rdfSource has failed", ex)
        return null
    }

    return model
}

private fun addConceptEntryNodeToModel(conceptEntry: Element, doc: Document, collectionResource: Resource,
                                       publisher: Resource?, model: Model) {
    val xpath = XPathFactory.newInstance().newXPath()
    val conceptUri = xpath.conceptUri(conceptEntry, doc)

    val conceptResource = model.createResource(conceptUri)
    val prefLabel = model.createResource()
    val definition = model.createResource()

    model.addConcept(conceptResource)
    model.addIdentifier(conceptResource, conceptEntry.getAttribute("id"))
    model.addCollectionMember(collectionResource, conceptResource)
    model.addPrefLabel(conceptResource, prefLabel)
    model.addDefinition(definition)
    model.addDescription(conceptResource, definition)
    model.addPublisher(conceptResource, publisher)

    xpath.langSections(conceptEntry, doc).forEach {
        val langEl = it as Element
        val lang = langEl.getAttribute("xml:lang")

        langEl.getElementsByTagName("descrip").forEach {
            it
                .let { it as Element }
                .let {
                    if(it.getAttribute("type").equals("definisjon")) {
                        model.addDefinitionLabel(it, definition, lang)
                    }
                }
        }

        langEl.getElementsByTagName("term").forEach {
            model.addPrefLabelLiteralForm(it as Element, prefLabel, lang)
        }
    }
}

private fun NodeList.forEach(action: (Node) -> Unit) {
    (0 until this.length)
        .asSequence()
        .map { this.item(it) }
        .forEach { action(it) }
}

/**
 * Extract collection URI from TBX content using XPath.
 * @param doc Dom document
 * @return Returns uri as String
 */
private fun XPath.collectionUri(doc: Document): String {
    val collectionUri = (compile("/tbx/tbxHeader/fileDesc/sourceDesc/p[@type='identifikator']")
        .evaluate(doc, XPathConstants.STRING) as String)
        .trim()

    return URI(collectionUri).toString()
}

/**
 * Extract collection name from TBX content using XPath.
 * @param doc Dom document
 * @return Returns name as string
 */
private fun XPath.collectionName(doc: Document): String =
    (compile("/tbx/tbxHeader/fileDesc/titleStmt/title")
        .evaluate(doc, XPathConstants.STRING) as String)
        .trim()

/**
 * Extract publisher from TBX content using XPath.
 * @param doc Dom document
 * @return Returns publisher uri as string
 */
private fun XPath.publisherUri(doc: Document, orgAdapter: OrganizationsAdapter): String? {
    val publisherId = (compile("/tbx/tbxHeader/fileDesc/sourceDesc/p[@type='ansvarligVirksomhet']")
        .evaluate(doc, XPathConstants.STRING) as String)
        .trim()

    val organization = orgAdapter.getOrganization(publisherId)
    return organization?.uri
}


/**
 * Extract conceptEntry elements from TBX content using XPath.
 * @param doc Dom document
 * @return Returns elements as NodeList
 */
private fun XPath.conceptEntries(doc: Document): NodeList =
    compile(
        "/tbx/text/body/conceptEntry")
        .evaluate(doc, XPathConstants.NODESET) as NodeList

/**
 * Extract concept URI from TBX content for specific conceptEntry using XPath.
 * @param conceptEntry ConceptEntry element
 * @param doc Dom document
 * @return Returns uri as String
 */
private fun XPath.conceptUri(conceptEntry: Element, doc: Document): String {
    val conceptUri = (compile(
        "/tbx/text/body/conceptEntry[@id='${conceptEntry.getAttribute("id")}']/admin[@type='identifikator']")
        .evaluate(doc, XPathConstants.STRING) as String)
        .trim()

    return URI(conceptUri).toString()
}

/**
 * Extract langSec elements from TBX content for specific conceptEntry using XPath.
 * @param conceptEntry ConceptEntry element
 * @param doc Dom document
 * @return Returns elements as NodeList
 */
private fun XPath.langSections(conceptEntry: Element, doc: Document): NodeList =
    compile("/tbx/text/body/conceptEntry[@id='${conceptEntry.getAttribute("id")}']/langSec")
        .evaluate(doc, XPathConstants.NODESET) as NodeList

/**
 * Set model namespace prefixes.
 */
private fun Model.setPrefixes(): Model {
    setNsPrefix("dct",DCTerms.getURI())
    setNsPrefix("skos",SKOS.getURI())
    setNsPrefix("skosxl",SKOSXL.getURI())
    setNsPrefix("vcard",VCARD.getURI())
    setNsPrefix("skosno", SKOSNO_NS)
    setNsPrefix("dcat",DCAT.getURI())
    setNsPrefix("rdfs", RDFS.getURI())
    return this
}

/**
 * Add concept resource to model.
 */
private fun Model.addConcept(resource: Resource) {
    add(
        createStatement(
            resource,
            RDF.type,
            SKOS.Concept)
    )
}

/**
 * Add publisher resource to model
 */
private fun Model.addPublisher(resource: Resource, publisher: RDFNode?) {
    publisher?.run {
        add(
            createStatement(
                resource,
                DCTerms.publisher,
                publisher)
        )
    }
}

/**
 * Add concept identifier to model.
 */
private fun Model.addIdentifier(concept: Resource, identifier: String) {
    add(
        createStatement(concept, DCTerms.identifier, identifier)
    )
}

/**
 * Add collection member to model.
 */
private fun Model.addCollectionMember(collectionResource: Resource, conceptResource: RDFNode) {
    add(
        createStatement(
            collectionResource,
            SKOS.member,
            conceptResource)
    )
}

/**
 * Add concept prefLabel to model.
 */
private fun Model.addPrefLabel(concept: Resource, prefLabel: RDFNode) {
    add(
        createStatement(concept, SKOSXL.prefLabel, prefLabel)
    )
}

/**
 * Add concept description to model.
 */
private fun Model.addDescription(concept: Resource, definition: RDFNode) {
    add(
        createStatement(
            concept,
            createProperty("${SKOSNO_NS}${SKOSNO_DEFINISJON_PROPERTY}"),
            definition)
    )
}

/**
 * Add concept definisjon to model.
 */
private fun Model.addDefinition(definition: Resource) {
    add(
        createStatement(
            definition,
            RDF.type,
            createResource("${SKOSNO_NS}${SKOSNO_DEFINISJON}"))
    )
}

/**
 * Add prefLabel literForm to model.
 */
private fun Model.addPrefLabelLiteralForm(el: Element, prefLabel: Resource, lang: String) {
    add(
        createStatement(
            prefLabel,
            SKOSXL.literalForm,
            createLiteral(el.textContent, lang)
        )
    )
}

/**
 * Add definition literForm to model.
 */
private fun Model.addDefinitionLabel(el: Element, definition: Resource, lang: String) {
    if(el.getAttribute("type").equals("definisjon")) {
        add(
            createStatement(
                definition,
                RDFS.label,
                createLiteral(el.textContent, lang)
            )
        )
    }
}




