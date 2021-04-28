package ardc.cerium.mycelium.rifcs.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
public class RegistryObject {
    private String key;

    private Collection collection;

    private Party party;

    private Service service;

    private Activity activity;

    private BaseRegistryObjectClass getBaseClass() {
        if (collection != null) {
            return collection;
        }else if (party != null) {
            return party;
        } else if (service != null) {
            return service;
        } else if (activity != null) {
            return activity;
        }

        // problem here, no class found
        return null;
    }

    public List<Identifier> getIdentifiers() {
        return getBaseClass() != null ? getBaseClass().getIdentifiers() : new ArrayList<>();
    }

    public List<RelatedObject> getRelatedObjects() {
        return getBaseClass() != null ? getBaseClass().getRelatedObjects() : new ArrayList<>();
    }

    public List<RelatedInfo> getRelatedInfos() {
        return getBaseClass() != null ? getBaseClass().getRelatedInfos() : new ArrayList<>();
    }
}
