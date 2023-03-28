package ardc.cerium.mycelium.util;

import ardc.cerium.core.exception.ContentNotSupportedException;
import ardc.cerium.mycelium.model.Vertex;
import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class VertexUtilTest {

	@Test
	void testResolveDOIVertex() {
        // given a vertex with empty title
        Vertex vertex = new Vertex("10.1126/science.169.3946.635", "doi");
        assertThat(vertex.getTitle()).isNull();

        // upon resolving
        VertexUtil.resolveVertex(vertex);

        // the title is updated to the right value fetched from DOI server
        assertThat(vertex.getTitle()).isEqualTo("The Structure of Ordinary Water");
        //The type is added as an meta field
        assertThat(vertex.getMetaAttribute("type"))
                .isEqualTo("journal-article");
        //The publisher is added as an meta field
        assertThat(vertex.getMetaAttribute("publisher"))
                .isEqualTo("American Association for the Advancement of Science (AAAS)");
	}

    @Test
    void testResolveUnknownDOIVertex() {
        // given a vertex with pretty much unresolvable value
        Vertex vertex = new Vertex("10.xxx/very-much-not-found", "doi");

        // upon resolving
        VertexUtil.resolveVertex(vertex);

        // the title is not changed, and there's no error
        assertThat(vertex.getTitle()).isNull();
    }

    @Test
	void testResolveORCIDVertex() {
        // given an orcid vertex with an empty title
        Vertex vertex = new Vertex("0000-0002-1825-0097", "orcid");
        vertex.setTitle("Unknown");

        // upon resolving
        VertexUtil.resolveVertex(vertex);

        // the title is updated to the right value fetched from ORCID server
        assertThat(vertex.getTitle()).isEqualTo("Josiah Carberry");

        // the rawTitle is saved in the meta
        assertThat(vertex.getMetaAttribute("rawTitle")).isEqualTo("Unknown");
	}

    @Test
    void testResolveRORVertex() {
        // given an ror vertex with an empty title
        Vertex vertex = new Vertex("031gd3q51", "ror");
        vertex.setTitle("Unknown");

        // upon resolving
        VertexUtil.resolveVertex(vertex);

        // the title is updated to the right value fetched from ROR server
        assertThat(vertex.getTitle()).isEqualTo("Conservatorio di Musica Giacomo Puccini");

        // the rawTitle is saved in the meta
        assertThat(vertex.getMetaAttribute("rawTitle")).isEqualTo("Unknown");
    }

    @Test
    void testResolveUnknownRORVertex() {
        // given an ror vertex with an empty title
        Vertex vertex = new Vertex("not-valid", "ror");


        // upon resolving
        VertexUtil.resolveVertex(vertex);

        // the title is not changed, and there's no error
        assertThat(vertex.getTitle()).isNull();
    }

    @Test
    void testResolveUnknownORCIDVertex() {
        // given a vertex with pretty much unresolvable value
        Vertex vertex = new Vertex("not-valid", "orcid");

        // upon resolving
        VertexUtil.resolveVertex(vertex);

        // the title is not changed, and there's no error
        assertThat(vertex.getTitle()).isNull();
    }


    @ParameterizedTest
    @CsvSource({
            // nla
            "http://nla.gov.au/nla.party-1692395,uri,nla.party-1692395,au-anl:peau",
            "http://nla.gov.au/nla.party-1692395,nla-party,nla.party-1692395,au-anl:peau",
            "http://nla.gov.au/nla.party-1692395,AU-VANDS,nla.party-1692395,au-anl:peau",
            "nla.gov.au/nla.party-1692395,AU-QGU,nla.party-1692395,au-anl:peau",
            "https://nla.gov.au/nla.party-1692395,AU-QUT,nla.party-1692395,au-anl:peau",
            "http://nla.gov.au/nla.party-1692395,nla.party,nla.party-1692395,au-anl:peau",
            "nla.party-1692395,AU-ANL:PEAU,nla.party-1692395,au-anl:peau",
            "nla.party-1692395,AU-QGU,nla.party-1692395,au-anl:peau",
            "1692395,NLA.PARTY,nla.party-1692395,au-anl:peau",

            // doi
            "DOI:10.234/455, doi, 10.234/455, doi",
            "http://doi.org/10.234/455, url, 10.234/455, doi",
            "https://doi.org/10.234/455, uri, 10.234/455, doi",
            "https://doi.org/10.234/455, doi, 10.234/455, doi",
            "10.234/455, doi, 10.234/455, doi",

            // ror
            "https://ror.org/01pmm8272, ror, ror.org/01pmm8272, ror",
            "01pmm8272, ror, ror.org/01pmm8272, ror",

            // random
            "1.234/455, fish, 1.234/455, fish",
            "http://doi.org/1.234/455, url, doi.org/1.234/455, url",

            // raid
            "http://hdl.handle.net/2328.1/1134, raid, 2328.1/1134, raid",

            // orcid
            "http://http://orcid.org/0000-0001-7212-0667,uri,0000-0001-7212-0667,orcid",
            "http://orcid.org/0000-0002-9539-5716,url,0000-0002-9539-5716,orcid",
            "https://orcid.org/0000-0002-9539-5716,url,0000-0002-9539-5716,orcid",
            "https://orcid.org/0000-0002-9539-5716/userInfo.csv,url,0000-0002-9539-5716,orcid",
            "0000-0002-9539-5716,orcid,0000-0002-9539-5716,orcid",
            "http://orcid.org/index.php,url,orcid.org/index.php,url",
            "http://forcid.org/9539-5716,url,forcid.org/9539-5716,url",

            // handles
            "http://handle.westernsydney.edu.au:8081/1959.7/512474, uri, 1959.7/512474, handle",
            "hdl:1959.7/512474, handle, 1959.7/512474, handle",
            "hdl:1959.7/512474, global, 1959.7/512474, handle",
            "hdl.handle.net/1959.7/512474, url, 1959.7/512474, handle",
            "https://hdl.handle.net/1959.7/512474, uri, 1959.7/512474, handle",
            "http://hdl.handle.net/1959.7/512474, handle, 1959.7/512474, handle",
            "1959.7/512474, handle, 1959.7/512474, handle",

            // uri
            "http://researchdata.ands.org.au/view/?key=http://hdl.handle.net/1959.14/201435, uri, researchdata.ands.org.au/view/?key=http://hdl.handle.net/1959.14/201435, uri",

            // purls
            "http://purl.org/au-research/grants/nhmrc/GNT1002592, uri, https://purl.org/au-research/grants/nhmrc/GNT1002592, purl",
            "http://purl.org/au-research/grants/nhmrc/GNT1002592, purl, https://purl.org/au-research/grants/nhmrc/GNT1002592, purl",
            "https://purl.org/au-research/grants/nhmrc/GNT1002592, global, https://purl.org/au-research/grants/nhmrc/GNT1002592, purl",
            "https://purl.org/au-research/grants/nhmrc/GNT1002592, url, https://purl.org/au-research/grants/nhmrc/GNT1002592, purl",

            // igsn
            "http://igsn.org/AU1243, igsn, AU1243, igsn",
            "https://igsn.org/AU1243, igsn, AU1243, igsn",
            "hdl.handle.net/10273/AU1243, handle, AU1243, igsn",
            "10273/AU1243, igsn, AU1243, igsn",
            "au1243, igsn, AU1243, igsn",
            "https://igsn.org/AU1243, uri, AU1243, igsn",

            // removal https protocol
            "http://geoserver-123.aodn.org.au/geoserver/ncwms?SERVICE=WMS&VERSION=1.3.0&REQUEST=GetCapabilities, url, geoserver-123.aodn.org.au/geoserver/ncwms?SERVICE=WMS&VERSION=1.3.0&REQUEST=GetCapabilities, url",
            "https://geoserver.imas.utas.edu.au/geoserver/wms?SERVICE=WMS&VERSION=1.3.0&REQUEST=GetCapabilities, uri, geoserver.imas.utas.edu.au/geoserver/wms?SERVICE=WMS&VERSION=1.3.0&REQUEST=GetCapabilities, uri",
            "http://google.com, local, google.com, local",
            "fish.org, global, fish.org, global",
            "https://fish.org?url=http://google.com, noidea, fish.org?url=http://google.com, noidea",
            "fish.org?url=http://google.com, uri, fish.org?url=http://google.com, uri",
    })
    void testNormaliseVertex(String preValue, String preType, String expectedValue, String expectedType) {
        Vertex vertex = new Vertex(preValue, preType);
        VertexUtil.normalise(vertex);
        assertThat(vertex.getIdentifier()).isEqualTo(expectedValue);
        assertThat(vertex.getIdentifierType()).isEqualTo(expectedType);
        assertThat(vertex.getMetaAttribute("rawIdentifierValue")).isEqualTo(preValue);
        assertThat(vertex.getMetaAttribute("rawIdentifierType")).isEqualTo(preType);
    }
}