package ardc.cerium.igsn.validator;

import ardc.cerium.igsn.TestHelper;
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
import ardc.cerium.core.common.util.Helpers;
import ardc.cerium.core.exception.ContentNotSupportedException;
import ardc.cerium.core.exception.VersionContentAlreadyExistsException;
import org.junit.Assert;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.IOException;
import java.util.Collections;

import static org.assertj.core.api.Java6Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = { SchemaService.class, VersionContentValidator.class })
class VersionContentValidatorTest {

	@Autowired
	SchemaService schemaService;

	@MockBean
	IdentifierService identifierService;

	@Test
	@DisplayName("New Version is considered new content")
	void isVersionNewContent() {
		VersionContentValidator versionContentValidator = new VersionContentValidator(identifierService, schemaService);
		Version version = TestHelper.mockVersion();
		Assert.assertTrue(versionContentValidator.isVersionNewContent("fish", version, "iFish"));
	}

	@Test
	@DisplayName("IsVersionNewContent comparing hash throws VersionContentAlreadyExistsException")
	void isVersionNewContent_ContentAlreadyExisted() throws IOException {
		VersionContentValidator versionContentValidator = new VersionContentValidator(identifierService, schemaService);
		Schema schema = schemaService.getSchemaByID(SchemaService.ARDCv1);

		Version version = TestHelper.mockVersion();
		// due to the nature of the FragmentProvider and the hash comparison, Fragment
		// always return a slightly differnet formatted content
		String validXML = Helpers.readFile("src/test/resources/xml/sample_ardcv1.xml");
		FragmentProvider fragmentProvider = (FragmentProvider) MetadataProviderFactory.create(schema,
				Metadata.Fragment);
		String original = fragmentProvider.get(validXML, 0);
		version.setContent(original.getBytes());
		version.setHash(VersionService.getHash(original));
		IdentifierProvider provider = (IdentifierProvider) MetadataProviderFactory.create(schema, Metadata.Identifier);
		String identifierValue = provider.get(validXML);
		Assert.assertThrows(VersionContentAlreadyExistsException.class, () -> {
			versionContentValidator.isVersionNewContent(original, version, identifierValue);
		});
	}

	@Test
	@DisplayName("When a version has the exact same content as the new content, throws VersionContentAlreadyExistsException")
	void isNewContent_VersionContentAlreadyExist_throwsException() {
		VersionContentValidator versionContentValidator = new VersionContentValidator(identifierService, schemaService);
		Version version = TestHelper.mockVersion();
		String oldContent = "fish";
		version.setContent(oldContent.getBytes());
		version.setHash(VersionService.getHash(version));
		Assert.assertThrows(VersionContentAlreadyExistsException.class, () -> {
			versionContentValidator.isVersionNewContent("fish", version, "iFish");
		});
	}

	@Test
	@DisplayName("No identifier exist, returns true")
	void isIdentifierNewContent_NoneExists() {
		VersionContentValidator versionContentValidator = new VersionContentValidator(identifierService, schemaService);
		String identifier = "20.500.11812/XXAB001QX";
		String schemaID = SchemaService.ARDCv1;
		String newContent = "<resources></resources>";
		assertThat(versionContentValidator.isIdentifierNewContent(newContent, identifier, schemaID)).isTrue();
	}

	@Test
	@DisplayName("Identifier exist, bound to a Record with a version of a different schema, returns true")
	void isIdentifierNewContent_RecordExistsButNotVersion() {
		VersionContentValidator versionContentValidator = new VersionContentValidator(identifierService, schemaService);

		String newContent = "something";
		String identifierValue = "20.500.11812/XXAB001QX";
		String schemaID = SchemaService.ARDCv1;

		// there is already an identifier exist, bound to a Record with a version of a
		// different schema
		Record record = TestHelper.mockRecord();
		Version version = TestHelper.mockVersion(record);
		version.setSchema(SchemaService.CSIROv3);
		record.setCurrentVersions(Collections.singletonList(version));
		Identifier identifier = TestHelper.mockIdentifier(record);
		Mockito.when(identifierService.findByValueAndType(identifierValue, Identifier.Type.IGSN))
				.thenReturn(identifier);

		assertThat(versionContentValidator.isIdentifierNewContent(newContent, identifierValue, schemaID)).isTrue();
	}

	@Test
	@DisplayName("Identifier exists, bound to a Record with a version of a the same schema, older content, returns true")
	void isIdentifierNewContent_VersionExistButNewContent() {
		VersionContentValidator versionContentValidator = new VersionContentValidator(identifierService, schemaService);

		String newContent = "NEW";
		String identifierValue = "20.500.11812/XXAB001QX";
		String schemaID = SchemaService.ARDCv1;

		// there is already an identifier exist, bound to a Record with a version of a
		// different schema
		Record record = TestHelper.mockRecord();
		Version version = TestHelper.mockVersion(record);
		version.setContent("OLD".getBytes());
		version.setSchema(SchemaService.ARDCv1);
		record.setCurrentVersions(Collections.singletonList(version));
		Identifier identifier = TestHelper.mockIdentifier(record);
		Mockito.when(identifierService.findByValueAndType(identifierValue, Identifier.Type.IGSN))
				.thenReturn(identifier);

		assertThat(versionContentValidator.isIdentifierNewContent(newContent, identifierValue, schemaID)).isTrue();
	}

	@Test
	@DisplayName("New content is new when the payload is new and the schema is recognisable")
	void isNewContent() throws IOException {
		VersionContentValidator versionContentValidator = new VersionContentValidator(identifierService, schemaService);
		String validXML = Helpers.readFile("src/test/resources/xml/sample_ardcv1.xml");
		Assert.assertTrue(versionContentValidator.isNewContent(validXML));
	}

	@Test
	@DisplayName("Throws ContentNotSupportedException when it's a schema that is not supported")
	void isNewContent_ContentNotSupported() {
		VersionContentValidator versionContentValidator = new VersionContentValidator(identifierService, schemaService);
		Assert.assertThrows(ContentNotSupportedException.class, () -> {
			versionContentValidator.isNewContent("fish");
		});
		Assert.assertThrows(ContentNotSupportedException.class, () -> {
			String validXML = Helpers.readFile("src/test/resources/xml/shiporder.xml");
			versionContentValidator.isNewContent(validXML);
		});
	}

	@Test
	void isNewContent_notNewContent() throws IOException {
		VersionContentValidator versionContentValidator = new VersionContentValidator(identifierService, schemaService);
		Schema schema = schemaService.getSchemaByID(SchemaService.ARDCv1);

		// due to the nature of the FragmentProvider and the hash comparison, Fragment
		// always return a slightly differnet formatted content
		String validXML = Helpers.readFile("src/test/resources/xml/sample_ardcv1.xml");
		FragmentProvider fragmentProvider = (FragmentProvider) MetadataProviderFactory.create(schema,
				Metadata.Fragment);
		String original = fragmentProvider.get(validXML, 0);

		// there is already an identifier exist, bound to a Record with the same version
		// of the same schema
		Record record = TestHelper.mockRecord();
		Version version = TestHelper.mockVersion(record);
		version.setContent(original.getBytes());
		version.setSchema(SchemaService.ARDCv1);
		version.setHash(VersionService.getHash(original));
		record.setCurrentVersions(Collections.singletonList(version));
		Identifier identifier = TestHelper.mockIdentifier(record);
		Mockito.when(identifierService.findByValueAndType("10273/XX0TUIAYLV", Identifier.Type.IGSN))
				.thenReturn(identifier);

		Assert.assertThrows(VersionContentAlreadyExistsException.class, () -> {
			versionContentValidator.isNewContent(original);
		});
	}

}