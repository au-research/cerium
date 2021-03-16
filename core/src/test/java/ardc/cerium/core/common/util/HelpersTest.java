package ardc.cerium.core.common.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.File;
import java.util.Date;

import ardc.cerium.core.exception.ContentNotSupportedException;
import org.junit.Assert;
import org.junit.jupiter.api.Test;

public class HelpersTest {

	@Test
	public void getContentType_xml() {

		try {
			String content_type = Helpers.probeContentType(new File("src/test/resources/xml/sample_igsn_csiro_v3.xml"));
			assertEquals(new String("application/xml"), content_type);
		}
		catch (Exception e) {
			System.out.println(e.getMessage());
		}

	}

	@Test
	public void getFileExtension_xml() {
		try {
			String content = Helpers.readFile("src/test/resources/xml/sample_igsn_csiro_v3.xml");
			String fileExt = Helpers.getFileExtensionForContent(content);
			assertEquals(new String(".xml"), fileExt);
		}
		catch (Exception e) {
			System.out.println(e.getMessage());
		}

	}

	@Test
	public void getContentType_json() {
		try {
			String content_type = Helpers.probeContentType(new File("src/test/resources/json/json_ld.json"));
			assertEquals(new String("application/json"), content_type);
		}
		catch (Exception e) {
			System.out.println(e.getMessage());
		}
	}

	@Test
	void convertDate() {
		String ISO8601Date = "2011-12-03";
		String ISO8601DateTime = "2020-09-27T12:56:47Z";

		Date newDate = Helpers.convertDate(ISO8601Date);
		assertThat(newDate).isInstanceOf(Date.class);

		Date newDateTime = Helpers.convertDate(ISO8601DateTime);
		assertThat(newDateTime).isInstanceOf(Date.class);
	}

	@Test
	void convertDateYYYY() {
		String ISO8601Date = "2011";

		Date newDate = Helpers.convertDate(ISO8601Date);
		assertThat(newDate).isInstanceOf(Date.class);
		assertThat(newDate).hasDayOfMonth(01);
		assertThat(newDate).hasMonth(1);

	}

	@Test
	void convertDateYYYYMM() {
		String ISO8601Date = "2011-12";

		Date newDate = Helpers.convertDate(ISO8601Date);
		assertThat(newDate).isInstanceOf(Date.class);
		assertThat(newDate).hasDayOfMonth(01);

	}


    @Test
	public void getContentType_text() {
		try {
			String content_type = Helpers.probeContentType(new File("src/test/resources/data/ardc.cerium.core.igsn.txt"));
			assertEquals(new String("text/plain"), content_type);
		}
		catch (Exception e) {
			System.out.println(e.getMessage());
		}
	}

	@Test
	public void testCheckFileSize(){
		long fiveMB = 5 * 1024 * 1024;
		Assert.assertThrows(ContentNotSupportedException.class, () -> {
			Helpers.checkFileSize("src/test/resources/xml/large_9_mb_payload.xml", fiveMB);
		});
	}

    @Test
	void testGetLine() {
		String multiLinePayload = "hello\nI'm a multilined\nmessage";
		String singleLinePayload = "hello I'm a multilined message";
		String result = Helpers.getLine(multiLinePayload, 2);
		assertThat(result.equals("message"));
		result = Helpers.getLine(multiLinePayload, -1);
		assertThat(result == null);
		result = Helpers.getLine(multiLinePayload, 11);
		assertThat(result == null);

		result = Helpers.getLine(singleLinePayload, 0);
		assertThat(result.equals(singleLinePayload));
		result = Helpers.getLine(singleLinePayload, 1);
		assertThat(result == null);
	}

}