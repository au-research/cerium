package ardc.cerium.igsn.service;

import ardc.cerium.core.common.dto.mapper.VersionMapper;
import ardc.cerium.core.common.entity.Record;
import ardc.cerium.core.common.entity.Version;
import ardc.cerium.core.common.model.User;
import ardc.cerium.core.common.repository.VersionRepository;
import ardc.cerium.core.common.service.RecordService;
import ardc.cerium.core.common.service.SchemaService;
import ardc.cerium.core.common.service.ValidationService;
import ardc.cerium.core.common.service.VersionService;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.UUID;

@Service("IGSNVersionService")
@ConditionalOnProperty(name = "app.igsn.enabled")
public class IGSNVersionService {

	@Autowired
	VersionService versionService;

	/**
	 * End the life of a {@link Version} The registry supports soft deleting of a
	 * {@link Version} so it's recommended to use this method to end the effective use of
	 * that {@link Version}
	 * @param version the {@link Version} to end
	 * @param userID the userID of the {@link User} to end it with
	 * @return the ended {@link Version}
	 */
	public Version end(@NotNull Version version, UUID userID) {
		version.setEndedAt(new Date());
		version.setCurrent(false);
		version.setEndedBy(userID);
		versionService.save(version);
		return version;
	}

	/**
	 * End the life of a {@link Version} The registry supports soft deleting of a
	 * {@link Version} so it's recommended to use this method to end the effective use of
	 * that {@link Version} with a given end date
	 * @param version the {@link Version} to end
	 * @param userID the userID of the {@link User} to end it with
	 * @param endedAt the Date when the version was ended
	 * @return the ended {@link Version}
	 */
	public Version end(@NotNull Version version, UUID userID, Date endedAt) {
		version.setEndedAt(endedAt);
		version.setCurrent(false);
		version.setEndedBy(userID);
		versionService.save(version);
		return version;
	}

	public Version save(Version version) {
		return versionService.save(version);
	}

	public Version getCurrentVersionForRecord(Record record, String supportedSchema) {
		return versionService.findVersionForRecord(record, supportedSchema);
	}

}
