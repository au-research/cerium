package ardc.cerium.igsn.client;

import ardc.cerium.igsn.TestHelper;
import ardc.cerium.core.exception.MDSClientConfigurationException;
import ardc.cerium.igsn.model.IGSNAllocation;
import org.junit.Assert;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;


class MDSClientTest {

    @Test
    @DisplayName("Test for ConfigurationException if MDS password is not set")
    void configurationException_pwd()
    {

        IGSNAllocation ia = TestHelper.mockIGSNAllocation();
        ia.setName("Faulty Allocation");
        ia.setMds_password(null);
        Assert.assertThrows(MDSClientConfigurationException.class, () -> {
            MDSClient mc = new MDSClient(ia);
        });
        try{
            MDSClient mc = new MDSClient(ia);
        }catch(Exception e){
            String msg = e.getMessage();
            Assert.assertEquals("MDS Password is not set for Allocation Faulty Allocation", msg);
        }
    }

    @Test
    @DisplayName("Test for ConfigurationException if MDS url is not set")
    void configurationException_url()
    {

        IGSNAllocation ia = TestHelper.mockIGSNAllocation();
        ia.setName("Faulty Allocation");
        ia.setMds_url("");
        Assert.assertThrows(MDSClientConfigurationException.class, () -> {
            MDSClient mc = new MDSClient(ia);
        });
        try{
            MDSClient mc = new MDSClient(ia);
        }catch(Exception e){
            String msg = e.getMessage();
            Assert.assertEquals("MDS URL is not set for Allocation Faulty Allocation", msg);
        }
    }

    @Test
    @DisplayName("Test for ConfigurationException if MDS username is not set")
    void configurationException_userNamel()
    {

        IGSNAllocation ia = TestHelper.mockIGSNAllocation();
        ia.setName("Faulty Allocation");
        ia.setMds_username("     ");
        Assert.assertThrows(MDSClientConfigurationException.class, () -> {
            MDSClient mc = new MDSClient(ia);
        });
        try{
            MDSClient mc = new MDSClient(ia);
        }catch(Exception e){
            String msg = e.getMessage();
            Assert.assertEquals("MDS Username is not set for Allocation Faulty Allocation", msg);
        }
    }


}