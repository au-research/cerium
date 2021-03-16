package ardc.cerium.drvs.service;


import ardc.cerium.drvs.model.DRVSSubmission;
import ardc.cerium.drvs.util.CSVUtil;
import ardc.cerium.core.common.entity.Request;
import ardc.cerium.core.common.model.*;
import ardc.cerium.core.common.model.schema.PlainTextValidator;
import ardc.cerium.core.common.util.Helpers;
import ardc.cerium.core.exception.ContentNotSupportedException;
import org.jetbrains.annotations.NotNull;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * Service for validating DRVS Requests
 */
@Service
@ConditionalOnProperty(name = "app.drvs.enabled")
public class DRVSRequestValidationService {

    /**
     * Validates a Request. Will throw Exceptions if content Not supported
     * @param request the {@link Request} that is pre-populated with required Attributes
     * and Payload
     * @throws ContentNotSupportedException when the payload content type is not supported
     */
    public void validate(@NotNull Request request) throws ContentNotSupportedException{
        String payloadPath = request.getAttribute(Attribute.PAYLOAD_PATH);

        File file = new File(payloadPath);
        String contentType;
        String csvContent;
        try{
            contentType = Helpers.probeContentType(file);
            csvContent = Helpers.readFile(payloadPath);
        }catch (IOException e){
            throw new ContentNotSupportedException("Inaccessible CSV file");
        }
        // sometimes CSV appears as text/plain is file extension not defined correctly
        List<String> supportedContentTypes = Arrays.asList("text/plain", "text/csv");

        if (!supportedContentTypes.contains(contentType)) {
            throw new ContentNotSupportedException("The content doesn't appear to be a CSV file");
        }

        List<DRVSSubmission> submissions = CSVUtil.readCSV(csvContent);
        if(submissions.isEmpty()){
            throw new ContentNotSupportedException("Unable to load content or empty CSV file provided");
        }

        for(DRVSSubmission submission:submissions){
            String localIdentifier = submission.getLocalCollectionID();
            // just check if there is a local collection Id is set
            // that means the CSV is loaded successfully
            if(localIdentifier == null){
                throw new ContentNotSupportedException("File doesn't appear to contain DRVS Submissions");
            }else{
                return;
            }
        }
    }
}
