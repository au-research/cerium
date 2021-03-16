package ardc.cerium.drvs.client;

import ardc.cerium.drvs.exception.DOINotFoundException;
import ardc.cerium.drvs.exception.DataCiteClientConfigurationException;
import org.junit.Assert;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
class DataCiteClientTest {

    @Test
    @DisplayName("Throws Configuration exception if DataCite url is not specified")
    void throw_exception_if_url_is_missing() throws Exception
    {
        Assert.assertThrows(DataCiteClientConfigurationException.class, () -> {
            DataCiteClient dataCiteClient = new DataCiteClient("");
        });
    }


    @Test
    @DisplayName("Fetches the DOI XML from DataCite")
    void fetch_a_doi() throws Exception
    {
        DataCiteClient dataCiteClient = new DataCiteClient("https://api.datacite.org/dois/");
        String doiXML = dataCiteClient.getDOIMetadata("10.5284/1015681");
        //System.out.println(doiXML);
        Assert.assertTrue(doiXML.contains("<identifier identifierType=\"DOI\">10.5284/1015681</identifier>"));
    }

    @Test
    @DisplayName("If Server not responding throw regular Exception")
    void server_not_responding() throws Exception
    {
        DataCiteClient dataCiteClient = new DataCiteClient("https://aaaaapi.datacite.org/dois/");
        Assert.assertThrows(Exception.class, () -> {
            String doiXML = dataCiteClient.getDOIMetadata("10.5284/1015681");
        });
    }


    @Test
    @DisplayName("If DOI not found on server throw DOINotFoundException")
    void fails_to_fetch_a_doi_not_found() throws Exception
    {
        DataCiteClient dataCiteClient = new DataCiteClient("https://api.datacite.org/dois/");
        Assert.assertThrows(DOINotFoundException.class, () -> {
            String doiXML = dataCiteClient.getDOIMetadata("10.5284/1015681999999999999999999999");
        });
    }

    @Test
    @DisplayName("Fetches the DOI XML from DataCite even if Identifier contains doi.org")
    void fetch_a_full_url_doi() throws Exception
    {
        DataCiteClient dataCiteClient = new DataCiteClient("https://api.datacite.org/dois/");
        String doiXML = dataCiteClient.getDOIMetadata("https://doi.org/10.5284/1015681");
        //System.out.println(doiXML);
        Assert.assertTrue(doiXML.contains("<identifier identifierType=\"DOI\">10.5284/1015681</identifier>"));

        doiXML = dataCiteClient.getDOIMetadata("http://doi.org/10.5284/1015681");
        //System.out.println(doiXML);
        Assert.assertTrue(doiXML.contains("<identifier identifierType=\"DOI\">10.5284/1015681</identifier>"));
        
    }



    @Test
    @DisplayName("Fails to fetch DOI XML from DataCite if figshare url is given as an Identifier")
    void fail_to_fetch_a_doi()
    {
        DataCiteClient dataCiteClient = new DataCiteClient("https://api.datacite.org/dois/");
        Assert.assertThrows(Exception.class, () -> {
            String doiXML = dataCiteClient.getDOIMetadata("https://figshare.com/articles/_/9968507");
        });
    }
}