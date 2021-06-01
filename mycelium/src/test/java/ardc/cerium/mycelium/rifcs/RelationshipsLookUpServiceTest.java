package ardc.cerium.mycelium.rifcs;

import ardc.cerium.mycelium.model.RelationLookupEntry;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class RelationshipsLookUpServiceTest {


    @Test
    void findanEntry(){
        RelationshipsLookUpService relationshipsLookUpService = new RelationshipsLookUpService();
        RelationLookupEntry relationLookupEntry = relationshipsLookUpService.getEntry("is   Part   Of");
        assertThat(relationLookupEntry).isNotNull();
        assertThat(relationLookupEntry.getReverseRelationType()).isEqualTo("hasPart");
        relationLookupEntry = relationshipsLookUpService.getEntry("Has part ici pant");
        assertThat(relationLookupEntry).isNotNull();
        assertThat(relationLookupEntry.getReverseRelationType()).isEqualTo("isParticipantIn");
    }
}