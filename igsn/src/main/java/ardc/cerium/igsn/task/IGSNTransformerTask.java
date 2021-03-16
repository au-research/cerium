package ardc.cerium.igsn.task;



import ardc.cerium.core.common.entity.Identifier;
import ardc.cerium.core.common.entity.Record;
import ardc.cerium.core.common.entity.Version;
import ardc.cerium.core.common.model.Schema;
import ardc.cerium.core.common.service.SchemaService;
import ardc.cerium.core.common.service.VersionService;
import ardc.cerium.core.common.transform.Transformer;
import ardc.cerium.core.common.transform.TransformerFactory;
import ardc.cerium.core.exception.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class IGSNTransformerTask implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(IGSNTransformerTask.class);

    private final Record record;

    private final Identifier identifier;

    private final VersionService versionService;

    private final SchemaService schemaService;

    private final Schema fromSchema;

    private final Schema toSchema;

    private final Map<String, String> parameters;

    public IGSNTransformerTask(Identifier identifier, VersionService versionService, SchemaService schemaService,
                               String fromSchema, String toSchema, Map<String, String> parameters) {
        this.identifier = identifier;
        this.record = identifier.getRecord();
        this.versionService = versionService;
        this.schemaService = schemaService;
        this.fromSchema = schemaService.getSchemaByID(fromSchema);
        this.toSchema = schemaService.getSchemaByID(toSchema);
        this.parameters = parameters;
    }

    /**
     * Transfrom between different schema if available
     */
    @Override
    public void run() {
        // obtain the version
        // obtain latest version fromSchema.getId()
        Version sourceVersion = versionService.findVersionForRecord(record, fromSchema.getId());


        if (sourceVersion == null) {
            logger.error("Unable to generate {} missing supported Schema version", toSchema.getId());
            throw new NotFoundException(String.format("Unable to generate %s missing supported Schema version for identifier %s",
                    toSchema.getId(), identifier.getValue()));
        }


        Transformer transformer = (Transformer) TransformerFactory.create(fromSchema, toSchema);

        if (parameters != null) {
            for (Map.Entry<String, String> entry : parameters.entrySet()) {
                if(entry.getValue() != null){
                    transformer.setParam(entry.getKey(), entry.getValue());
                }
            }
        }

        logger.debug("IGSNTransformer from {} to {} obtained", fromSchema.getId(), toSchema.getId());

        try {
            Version newVersion = transformer.transform(sourceVersion);
            String hash = VersionService.getHash(newVersion);
            // check if there's existing current toSchema.getId() and if they're different
            Version existingVersion = versionService.findVersionForRecord(record, toSchema.getId());
            if (existingVersion != null) {
                if (!existingVersion.getHash().equals(hash)) {
                    existingVersion.setHash(hash);
                    existingVersion.setContent(newVersion.getContent());
                    existingVersion.setCreatedAt(sourceVersion.getCreatedAt());
                    existingVersion.setRequestID(sourceVersion.getRequestID());
                    versionService.save(existingVersion);
                } else {
                    logger.debug("{} Content didn't change for version with identifier {}, skipping",
                             toSchema.getId(), identifier.getValue());
                    return;
                }
            }else{
                newVersion.setRequestID(sourceVersion.getRequestID());
                newVersion.setCreatedAt(sourceVersion.getCreatedAt());
                newVersion.setRecord(record);
                newVersion.setCurrent(true);
                newVersion.setHash(hash);
                versionService.save(newVersion);
            }

            logger.info("Processed {} transformation for record with identifier {}", toSchema.getId(), identifier.getValue());
        }
        catch (Exception e) {
            e.printStackTrace();
            logger.error("Error transforming {} for record = {} reason: {}", toSchema.getId(), identifier.getValue(), e.getMessage());
        }
    }

}
