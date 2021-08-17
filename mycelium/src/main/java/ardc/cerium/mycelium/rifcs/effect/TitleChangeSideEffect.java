package ardc.cerium.mycelium.rifcs.effect;

public class TitleChangeSideEffect extends SideEffect{

    private final String registryObjectId;

    private final String oldTitle;

    private final String newTitle;

    public TitleChangeSideEffect(String registryObjectId, String beforeTitle, String afterTitle) {
        this.registryObjectId = registryObjectId;
        this.oldTitle = beforeTitle;
        this.newTitle = afterTitle;
    }

    @Override
    public void handle() {
        // updates oldTitle to newTitle for documents in SOLR relationships collection
        // updates related_$class_title and search fields in SOLR portal collection
    }
}
