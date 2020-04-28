package no.ccat;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;

public class SKOSNO_NEW {
    /**
     * The namespace of the SKOS-NO vocabulary as a string
     */
    public static final String uri = "https://data.norge.no/vocabulary/skosno#";
    /**
     * The RDF model that holds the SKOS-NO entities
     */
    private static final Model m = ModelFactory.createDefaultModel();
    /**
     * The namespace of the SKOS-NO vocabulary
     */
    public static final Resource NAMESPACE = m.createResource(uri);
    /* ##########################################################
     * Defines SKOS-NO Classes
       ########################################################## */
    public static final Resource Definition = m.createResource(uri + "Definisjon");
    /* ##########################################################
     * Defines SKOS-XL Properties
       ########################################################## */
    // TODO: remove betydningsbeskrivelse as it is only an abstract class (https://doc.difi.no/data/begrep-skos-ap-no/#_betydningsbeskrivelse_tekst_tekst)
    public static final Property betydningsbeskrivelse = m.createProperty(uri + "betydningsbeskrivelse");
    public static final Property definisjon = m.createProperty(uri + "definisjon");
    public static final Property bruksområde = m.createProperty( uri + "bruksområde");
    public static final Property omfang = m.createProperty( uri + "omfang");
    public static final Property forholdTilKilde = m.createProperty(uri + "forholdTilKilde");

    /**
     * Returns the namespace of the SKOS-NO schema as a string
     *
     * @return the namespace of the SKOS-NO schema
     */
    public static String getURI() {
        return uri;
    }
}
