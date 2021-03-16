package ardc.cerium.igsn.service;

import ardc.cerium.core.common.entity.Record;
import org.jetbrains.annotations.NotNull;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "app.igsn.enabled")
public class IGSNRecordService {

	public static final String recordType = "IGSN";

	/**
	 * Creates a custom record for the Record.type = IGSN
	 * @return a prebuilt {@link Record} of type IGSN
	 */
	@NotNull
	public static Record create() {
		Record record = new Record();
		record.setType(recordType);
		return record;
	}

}
