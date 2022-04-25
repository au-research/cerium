package ardc.cerium.orcid;

public enum MetadataContentType {
    ORCID_JSON("application/orcid+json");

    private String contentType;

    MetadataContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getContentType() {
        return this.contentType;
    }
}
