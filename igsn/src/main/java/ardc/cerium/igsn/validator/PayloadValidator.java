package ardc.cerium.igsn.validator;

import ardc.cerium.core.common.model.User;
import ardc.cerium.core.common.service.IdentifierService;
import ardc.cerium.core.common.service.SchemaService;
import ardc.cerium.core.common.service.ValidationService;
import ardc.cerium.core.common.service.VersionService;
import ardc.cerium.core.exception.*;
import ardc.cerium.igsn.validator.ContentValidator;
import ardc.cerium.igsn.validator.UserAccessValidator;
import ardc.cerium.igsn.validator.VersionContentValidator;

import java.io.IOException;

/**
 * Payload Validator is the top level validator when records sent to the registry it
 * performs several validations Content validation to check if xml or json or csv is well
 * formed and valid User access validation checks if user has the desired access for the
 * allocations needed to complete the tasks if Update content then also checks content if
 * Record has a version with the given schema and it is different from the current one
 */
public class PayloadValidator {

	private final ContentValidator contentValidator;

	private final UserAccessValidator userAccessValidator;

	private final VersionContentValidator versionContentValidator;

	/**
	 * Constructor when the various validators are already defined elsewhere
	 * @param contentValidator {@link ContentValidator}
	 * @param versionContentValidator {@link VersionContentValidator}
	 * @param userAccessValidator {@link UserAccessValidator}
	 */
	public PayloadValidator(ContentValidator contentValidator, VersionContentValidator versionContentValidator,
			UserAccessValidator userAccessValidator) {
		this.contentValidator = contentValidator;
		this.userAccessValidator = userAccessValidator;
		this.versionContentValidator = versionContentValidator;
	}

	/**
	 * Constructor that builds it's own set of Validators. Since this is an instance and
	 * not a {@link org.springframework.stereotype.Service}. All Service object. needs to
	 * be passed in and can't be automatically injected.
	 * @param schemaService {@link SchemaService}
	 * @param validationService {@link ValidationService}
	 * @param identifierService {@link IdentifierService}
	 * @param versionService {@link VersionService}
	 */
	public PayloadValidator(SchemaService schemaService, ValidationService validationService,
			IdentifierService identifierService, VersionService versionService) {
		this.contentValidator = new ContentValidator(schemaService);
		this.userAccessValidator = new UserAccessValidator(identifierService, validationService, schemaService);
		this.versionContentValidator = new VersionContentValidator(identifierService, schemaService);
	}

	/**
	 * The {@link UserAccessValidator} holds a mutable state.
	 * @return The instance of {@link UserAccessValidator}
	 */
	public UserAccessValidator getUserAccessValidator() {
		return this.userAccessValidator;
	}

	/**
	 * @param content String the payload content as String
	 * @param user the logged in User who requested the mint / update
	 * @throws IOException and other type of exceptions by contentValidator and user
	 * access validator
	 */
	public void validateMintPayload(String content, User user) throws IOException, ContentNotSupportedException,
			XMLValidationException, JSONValidationException, ForbiddenOperationException {
		// validate the entire XML or JSON content
		contentValidator.validate(content);
		// check if the current user has insert or update access for the records with the
		// given identifiers
		userAccessValidator.canUserCreateIGSNRecord(content, user);
	}

	/**
	 * @param content String the payload content as String
	 * @param user the logged in User who requested the mint / update
	 * @throws IOException and other type of exceptions by contentValidator and user
	 * access validator
	 */
	public void validateUpdatePayload(String content, User user)
			throws IOException, ContentNotSupportedException, XMLValidationException, JSONValidationException,
			ForbiddenOperationException, VersionContentAlreadyExistsException {
		// validate the entire XML or JSON content
		contentValidator.validate(content);
		// check if the current user has insert or update access for the records with the
		// given identifiers
		userAccessValidator.canUserUpdateIGSNRecord(content, user);
		// check if the contents are new compared what stored in the registry
		versionContentValidator.isNewContent(content);
	}

}
