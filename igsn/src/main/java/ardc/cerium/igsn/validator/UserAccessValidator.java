package ardc.cerium.igsn.validator;

import ardc.cerium.core.common.entity.Identifier;
import ardc.cerium.core.common.entity.Record;
import ardc.cerium.core.common.model.Allocation;
import ardc.cerium.core.common.model.Schema;
import ardc.cerium.core.common.model.Scope;
import ardc.cerium.core.common.model.User;
import ardc.cerium.core.common.provider.IdentifierProvider;
import ardc.cerium.core.common.provider.Metadata;
import ardc.cerium.core.common.provider.MetadataProviderFactory;
import ardc.cerium.core.common.repository.IdentifierRepository;
import ardc.cerium.core.common.service.IdentifierService;
import ardc.cerium.core.common.service.SchemaService;
import ardc.cerium.core.common.service.ValidationService;
import ardc.cerium.core.exception.ContentNotSupportedException;
import ardc.cerium.core.exception.ContentProviderNotFoundException;
import ardc.cerium.core.exception.ForbiddenOperationException;
import ardc.cerium.igsn.model.IGSNAllocation;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class UserAccessValidator {

	private final ValidationService validationService;

	private final SchemaService schemaService;

	private final IdentifierService identifierService;

	private static final String IGSNallocationType = "urn:ardc:igsn:allocation";

	private UUID allocationID;

	public UserAccessValidator(IdentifierService identifierService, ValidationService validationService,
			SchemaService schemaService) {
		this.identifierService = identifierService;
		this.schemaService = schemaService;
		this.validationService = validationService;
	}

	/**
	 * Tests for each IGSN Identifier in the content test for existing Identifiers tests
	 * if user has access to allocations with the given identifier prefix and namespace
	 * @param content the entire payload XML or JSON
	 * @param user the user making the mint / update request
	 * @return true if user has access to all identifiers in the given payload
	 * @throws ContentNotSupportedException when content is not supported
	 * @throws ForbiddenOperationException when user has no access to the identifier
	 */
	public boolean canUserCreateIGSNRecord(String content, User user)
			throws ContentNotSupportedException, ForbiddenOperationException, ContentProviderNotFoundException {
		Schema schema = schemaService.getSchemaForContent(content);
		IdentifierProvider provider = (IdentifierProvider) MetadataProviderFactory.create(schema, Metadata.Identifier);
		List<String> identifiers = provider.getAll(content);
		return canUserCreateIGSNIdentifier(identifiers, user);
	}

	public boolean canUserCreateIGSNIdentifier(List<String> identifiers, User user) throws ForbiddenOperationException{
		String prefix = "######";
		String namespace = "######";
		IGSNAllocation igsnAllocation = null;
		List<String> identifierValues = new ArrayList<String>();
		for (String identifierValue : identifiers) {
			if (igsnAllocation == null) {
				// get the first record and make sure all other records has the same
				// prefix and namespace
				igsnAllocation = getIGSNAllocation(identifierValue, user, Scope.CREATE);
				if (igsnAllocation == null) {
					throw new ForbiddenOperationException(
							"User has no access to the given Identifier: " + identifierValue);
				}
				allocationID = igsnAllocation.getId();
				prefix = igsnAllocation.getPrefix();
				namespace = igsnAllocation.getNamespace();
			}
			else if (!identifierValue.startsWith(prefix + "/" + namespace)) {
				throw new ForbiddenOperationException(String.format("Mixed allocations are not supported. %s " +
						"doesn't match the prefix or namespace of the previous identifier. ", identifierValue));
			}
			else if (identifierValues.contains(identifierValue)) {
				throw new ForbiddenOperationException("Duplicated Identifier found in payload " + identifierValue);
			}
			identifierValues.add(identifierValue);
		}

		return true;
	}


	/**
	 * Tests for each IGSN Identifier in the content finds the records in the registry
	 * with eaach Identifier and checks if user has access to the records
	 * @param content the entire payload XML or JSON
	 * @param user the user making the mint / update request
	 * @return true if user has access to all Records (via their Identifier) contained in
	 * the payload
	 * @throws ContentNotSupportedException when content is not supported
	 * @throws ForbiddenOperationException when user has no access to the identifier
	 */
	public boolean canUserUpdateIGSNRecord(String content, User user)
			throws ContentNotSupportedException, ForbiddenOperationException, ContentProviderNotFoundException {
		Schema schema = schemaService.getSchemaForContent(content);
		IdentifierProvider provider = (IdentifierProvider) MetadataProviderFactory.create(schema, Metadata.Identifier);
		List<String> identifiers = provider.getAll(content);
		return canUserUpdateIGSNIdentifier(identifiers, user);
	}

	public boolean canUserUpdateIGSNIdentifier(List<String> identifiers, User user) throws ForbiddenOperationException{
		for (String identifierValue : identifiers) {
			Identifier existingIdentifier = identifierService.findByValueAndType(identifierValue, Identifier.Type.IGSN);
			if (existingIdentifier == null) {
				throw new ForbiddenOperationException("Identifier doesn't exists with value: " + identifierValue);
			}
			Record record = existingIdentifier.getRecord();
			if(record == null){
				throw new ForbiddenOperationException("Record doesn't exists for identifier: " + identifierValue);
			}
			if (!hasAccessToRecord(record, user)) {
				throw new ForbiddenOperationException("User has no access to the Record: " + record.getId());
			}
			allocationID = record.getAllocationID();
		}
		return true;
	}



	/**
	 * Finds the allocation that User has access to to operate with the given Identifier
	 * space
	 * @param identifierValue an IGSN identifier
	 * @param user and IGSN User
	 * @param scope {create | update}
	 * @return IGSNAllocation for the given identifier if the user has access to
	 */

	public IGSNAllocation getIGSNAllocation(String identifierValue, User user, Scope scope) {
		// get allocation from identifier that user has access to
		List<Allocation> allocations = user.getAllocationsByType(this.IGSNallocationType);
		for (Allocation allocation : allocations) {
			IGSNAllocation ia = (IGSNAllocation) allocation;
			String prefix = ia.getPrefix();
			String namespace = ia.getNamespace();
			if (identifierValue.startsWith(prefix + "/" + namespace) && ia.getScopes().contains(scope)) {
				return ia;
			}
		}
		return null;
	}

	public UUID getAllocationID() {
		return allocationID;
	}

	public boolean hasAccessToRecord(Record record, User user) {
		return validationService.validateRecordOwnership(record, user);
	}

}
