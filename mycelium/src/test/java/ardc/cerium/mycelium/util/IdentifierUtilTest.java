package ardc.cerium.mycelium.util;

import org.assertj.core.api.AssertionsForClassTypes;
import org.junit.jupiter.api.Test;

class IdentifierUtilTest {
    @Test
    void it_can_get_doi_url(){

        String url = IdentifierUtil.getUrl("10.7021/6547547", "doi");
        AssertionsForClassTypes.assertThat(url).isEqualTo("https://doi.org/10.7021/6547547");
    }

    @Test
    void it_can_get_orcid_url(){

        String url = IdentifierUtil.getUrl("0000-0002-5407-9987", "orcid");
        AssertionsForClassTypes.assertThat(url).isEqualTo("https://orcid.org/0000-0002-5407-9987");
    }

    @Test
    void it_can_get_handle_url(){

        String url = IdentifierUtil.getUrl("11057/id59416f87", "handle");
        AssertionsForClassTypes.assertThat(url).isEqualTo("https://hdl.handle.net/11057/id59416f87");
    }

    @Test
    void it_can_get_purl_url(){

        String url = IdentifierUtil.getUrl("https://purl.org/au-research/grants/arc/DP0451499", "purl");
        AssertionsForClassTypes.assertThat(url).isEqualTo("https://purl.org/au-research/grants/arc/DP0451499");
    }

    @Test
    void it_can_get_nla_url(){

        String url = IdentifierUtil.getUrl("nla.party-1504955", "AU-ANL:PEAU");
        AssertionsForClassTypes.assertThat(url).isEqualTo("https://nla.gov.au/nla.party-1504955");
    }

    @Test
    void it_can_get_igsn_url(){

        String url = IdentifierUtil.getUrl("AU1243", "igsn");
        AssertionsForClassTypes.assertThat(url).isEqualTo("https://igsn.org/AU1243");
    }

    @Test
    void it_can_get_uri_url(){

        String url = IdentifierUtil.getUrl("communities.research.uwa.edu.au/vivo/individual/dataset458", "uri");
        AssertionsForClassTypes.assertThat(url).isEqualTo("https://communities.research.uwa.edu.au/vivo/individual/dataset458");
    }

    @Test
    void it_can_get_scopusID_url(){

        String url = IdentifierUtil.getUrl("55253990100", "scopusID");
        AssertionsForClassTypes.assertThat(url).isEqualTo("https://www.scopus.com/authid/detail.uri?authorId=55253990100");
    }

    @Test
    void it_can_get_grid_url(){

        String url = IdentifierUtil.getUrl("grid.ac/institutes/grid.1005.4", "grid");
        AssertionsForClassTypes.assertThat(url).isEqualTo("https://grid.ac/institutes/grid.1005.4");
    }
    //RDA-704
    @Test
    void it_can_get_url(){
        String url = IdentifierUtil.getUrl("somewebsiteexample.com.id", "url");
        AssertionsForClassTypes.assertThat(url).isEqualTo("https://somewebsiteexample.com.id");
    }

}