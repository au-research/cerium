package ardc.cerium.drvs.service;

import ardc.cerium.drvs.TestHelper;
import ardc.cerium.core.common.entity.Request;
import ardc.cerium.core.common.model.Attribute;
import ardc.cerium.core.exception.ContentNotSupportedException;
import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = DRVSRequestValidationService.class)
@TestPropertySource(properties = "app.drvs.enabled=true")
class DRVSRequestValidationServiceTest {

    @Autowired
    DRVSRequestValidationService drvsRequestValidationService;

    @Test
    void validate_throwsExeption_1() {
        Request request = TestHelper.mockRequest();
        request.setAttribute(Attribute.PAYLOAD_PATH, "src/test/resources/xml/sample_ardcv1.xml");
        Assert.assertThrows(ContentNotSupportedException.class, () -> {
            drvsRequestValidationService.validate(request);
        });

    }

    @Test
    void validate_throwsExeption_2() {
        Request request = TestHelper.mockRequest();
        request.setAttribute(Attribute.PAYLOAD_PATH, "src/test/resources/data/igsn.txt");
        Assert.assertThrows(ContentNotSupportedException.class, () -> {
            drvsRequestValidationService.validate(request);
        });
    }

    @Test
    void validate_withoutExeption() {
        Request request = TestHelper.mockRequest();
        request.setAttribute(Attribute.PAYLOAD_PATH, "src/test/resources/data/drvs_test_data.csv");
        drvsRequestValidationService.validate(request);
    }
}