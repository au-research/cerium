package ardc.cerium.doi;

public enum MetadataContentType {
    RDF_XML("application/rdf+xml"),
    RDF_TURTLE("text/turtle"),
    CITEPROC_JSON("application/vnd.citationstyles.csl+json"),
    SCHEMA_ORG_JSONLD("application/vnd.schemaorg.ld+json"),
    FORMATTED_TEXT_CITATION("text/x-bibliography"),
    RIS("application/x-research-info-systems"),
    BIBTEX("application/x-bibtex"),
    CROSSREF_UNIXREF_XML("application/vnd.crossref.unixref+xml"),
    CROSSREF_UNIXSD_XML("application/vnd.crossref.unixsd+xml"),
    DATACITE_XML("application/vnd.datacite.datacite+xml"),
    ONIX_DOI("application/vnd.medra.onixdoi+xml");


    private String contentType;

    MetadataContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getContentType() {
        return this.contentType;
    }
}
