package ardc.cerium.drvs.util;

import ardc.cerium.core.common.util.Helpers;
import ardc.cerium.drvs.model.DRVSSubmission;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CSVUtilTest {

	@Test
	@DisplayName("When a CSV is read, it produces the DRVS Submission Record with the right fields")
	void readCSV() throws IOException {
		String inputString = Helpers.readFile("src/test/resources/data/drvs.csv");

		List<DRVSSubmission> results = CSVUtil.readCSV(inputString);
		assertThat(results).hasSize(1);

		DRVSSubmission submission = results.get(0);
		assertThat(submission.getLocalCollectionID()).isEqualTo("al33");
		assertThat(submission.getCollectionCapacity()).isEqualTo("1249");
		assertThat(submission.getApplicationResearchDisciplines()).isEqualTo(
				"0401 Atmospheric Sciences,0405 Oceanography,0399 Other Chemical Sciences, 0406 - Physical Geography and Environmental Geoscience");
		assertThat(submission.getTitle())
				.isEqualTo("Earth System Grid Federation (ESGF) Replicated CMIP5-era Datasets");
		assertThat(submission.getDescription()).isEqualTo(
				"This project contains Coupled Model Intercomparison Project phase 3 (CMIP3) replication data obtained from the ESGF.");
		assertThat(submission.getDataControllers()).isEqualTo("https://ror.org/04yx6dh41");
		assertThat(submission.getDOI()).isEmpty();
	}

	@Test
	@DisplayName("When a multi line csv is read, multiple DRVSSubmission is available")
	void readCSV_MultiLine() throws IOException {
		String inputString = Helpers.readFile("src/test/resources/data/drvs_multi.csv");
		List<DRVSSubmission> results = CSVUtil.readCSV(inputString);
		assertThat(results).hasSize(2);
	}

}