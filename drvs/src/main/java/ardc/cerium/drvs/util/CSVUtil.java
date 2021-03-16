package ardc.cerium.drvs.util;

import ardc.cerium.drvs.model.DRVSSubmission;
import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import com.opencsv.bean.HeaderColumnNameMappingStrategy;

import java.io.StringReader;
import java.util.List;

public class CSVUtil {

	/**
	 * Parse a CSV payload into a List of {@link DRVSSubmission}
	 * @param payload the CSV payload in String
	 * @return a {@link List} of {@link DRVSSubmission}
	 */
	public static List<DRVSSubmission> readCSV(String payload) {

		// removing the Byte Order Mark to be able to read the first column
		payload = payload.replace("\uFEFF", "");

		HeaderColumnNameMappingStrategy<DRVSSubmission> strategy = new HeaderColumnNameMappingStrategy<>();
		strategy.setType(DRVSSubmission.class);

		CsvToBean<DRVSSubmission> csvToBean = new CsvToBeanBuilder<DRVSSubmission>(new StringReader(payload))
				.withMappingStrategy(strategy).withIgnoreLeadingWhiteSpace(true).build();

		return csvToBean.parse();
	}

}
