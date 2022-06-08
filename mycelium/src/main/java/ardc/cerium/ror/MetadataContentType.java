package ardc.cerium.ror;

public enum MetadataContentType {
    ROR_JSON("application/json");

    private String contentType;

    MetadataContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getContentType() {
        return this.contentType;
    }
}
