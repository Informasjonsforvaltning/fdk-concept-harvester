package no.dcat.shared;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;

public class Schema {
    private static String NS = "http://schema.org/";
    private static Model model = ModelFactory.createDefaultModel();
    public static Property startDate;
    public static Property endDate;

    public Schema() {
    }

    static {
        startDate = model.createProperty(NS, "startDate");
        endDate = model.createProperty(NS, "endDate");
    }
}
