package ardc.cerium.mycelium.util;

import org.assertj.core.api.AssertionsForClassTypes;
import org.junit.jupiter.api.Test;

class IdentifierUtilTest {
    @Test
    void it_can_get_doi_url(){

        String url = IdentifierUtil.getUrl("10.7021/6547547", "doi");
        AssertionsForClassTypes.assertThat(url).isEqualTo("https://doi.org/10.7021/6547547");
    }

}