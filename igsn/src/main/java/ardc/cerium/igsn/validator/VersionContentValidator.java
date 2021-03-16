package ardc.cerium.igsn.validator;

import ardc.cerium.core.common.entity.Identifier;
import ardc.cerium.core.common.entity.Record;
import ardc.cerium.core.common.entity.Version;
import ardc.cerium.core.common.model.Schema;
import ardc.cerium.core.common.provider.FragmentProvider;
import ardc.cerium.core.common.provider.IdentifierProvider;
import ardc.cerium.core.common.provider.Metadata;
import ardc.cerium.core.common.provider.MetadataProviderFactory;
import ardc.cerium.core.common.service.IdentifierService;
import ardc.cerium.core.common.service.SchemaService;
import ardc.cerium.core.common.service.VersionService;
import ardc.cerium.core.exception.ContentProviderNotFoundException;
import ardc.cerium.core.exception.VersionContentAlreadyExistsException;
import ardc.cerium.igsn.validator.PayloadValidator;

import java.util.Optional;

/**
 * This validators validate an XML payload in the supported Schema. Used by
 * {@link PayloadValidator}
 */
public class VersionContentValidator {

	private final IdentifierService identifierService;

	private final SchemaService schemaService;

	public VersionContentValidator(IdentifierService identifierService, SchemaService schemaService) {
		this.identifierService = identifierService;
		this.schemaService = schemaService;
	}

	/**
	 * Checks if a Payload has all new content. Calls {@link #isIdentifierNewContent}
	 * internally for each Fragment found
	 * @param payload String payload to check, requires a supported {@link Schema}
	 * document
	 * @return true if the payload (all content) is considered new and ready for ingest.
	 * @throws VersionContentAlreadyExistsException bubble up from
	 * {@link #isVersionNewContent}
	 * @throws ContentProviderNotFoundException bubble up from {@link FragmentProvider}
	 * and {@link IdentifierProvider}
	 */
	public boolean isNewContent(String payload)
			throws VersionContentAlreadyExistsException, ContentProviderNotFoundException {
		Schema schema = schemaService.getSchemaForContent(payload);
		FragmentProvider fProvider = (FragmentProvider) MetadataProviderFactory.create(schema, Metadata.Fragment);
		IdentifierProvider iProvider = (IdentifierProvider) MetadataProviderFactory.create(schema, Metadata.Identifier);

		int numberOfFragments = fProvider.getCount(payload);
		for (int i = 0; i < numberOfFragments; i++) {
			String content = fProvider.get(payload, i);
			String identifier = iProvider.get(content);
			isIdentifierNewContent(content, identifier, schema.getId());
		}
		return true;
	}

	/**
	 * Checks if a Content is new for a given Identifier and schema. Calls
	 * {@link #isVersionNewContent(String, Version, String)} internally
	 * @param content String content to compare
	 * @param identifierValue the String value of the Identifier, only checks IGSN type
	 * @param schemaID the String schemaID
	 * @return true if the content is new for this Identifier
	 * @throws VersionContentAlreadyExistsException bubble up from
	 * {@link #isVersionNewContent(String, Version, String)}
	 */
	public boolean isIdentifierNewContent(String content, String identifierValue, String schemaID)
			throws VersionContentAlreadyExistsException {
		Identifier identifier = identifierService.findByValueAndType(identifierValue, Identifier.Type.IGSN);

		// if the identifier doesn't exist, this is new content
		if (identifier == null) {
			return true;
		}

		// if the identifier exists, we check if the content is new by the schema of the
		// version
		Record record = identifier.getRecord();
		Optional<Version> cVersion = record.getCurrentVersions().stream()
				.filter(version -> version.getSchema().equals(schemaID)).findFirst();

		// if there's no version of the given schema, this is new content
		if (!cVersion.isPresent()) {
			return true;
		}

		// check new content by version
		Version version = cVersion.get();
		return isVersionNewContent(content, version, identifierValue);
	}

	/**
	 * Checks if a content is new for a given {@link Version}. Comparison is done via
	 * VersionService.getHash
	 * @param content the new String content
	 * @param version the {@link Version} to check on
	 * @param identifierValue the identifier of the record
	 * @return true if the Content is considered new
	 * @throws VersionContentAlreadyExistsException when the version content already
	 * existed
	 */
	public boolean isVersionNewContent(String content, Version version, String identifierValue)
			throws VersionContentAlreadyExistsException {
		String versionHash = version.getHash();
		String incomingHash = VersionService.getHash(content);
		if (incomingHash.equals(versionHash)) {
			throw new VersionContentAlreadyExistsException(identifierValue, version.getSchema());
		}
		return true;
	}

}
