package ardc.cerium.drvs.dto;

import ardc.cerium.core.common.dto.RequestDTO;
import ardc.cerium.core.common.model.User;

import java.util.HashMap;
import java.util.Map;

public class DRVSRequestDTO extends RequestDTO {

    private Map<String, String> summary = new HashMap<>();

    private User creator;

    public void setSummary(Map<String, String> summary) {
        this.summary = summary;
    }

    @Override
    public Map<String, String> getSummary() {
        return summary;
    }

    public User getCreator() {
        return creator;
    }

    public void setCreator(User creator) {
        this.creator = creator;
    }
}
