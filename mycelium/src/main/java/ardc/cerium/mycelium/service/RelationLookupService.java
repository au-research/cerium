package ardc.cerium.mycelium.service;

import ardc.cerium.core.common.util.Helpers;
import ardc.cerium.mycelium.model.RelationLookupEntry;
import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import com.opencsv.bean.HeaderColumnNameMappingStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class RelationLookupService {

	/**
	 * The HashMap will store a mapping between the relationType and the corresponding
	 * RelationLookUpEntry.
	 *
	 * HashMap has O(1) lookup as long as the relationType is matched properly, compared
	 * to O(n) for List filters
	 */
	public static Map<String, RelationLookupEntry> lookupTable = new HashMap<>();

	public static String RELATIONS_CSV_RESOURCE_PATH = "relations.csv";

	@PostConstruct
	public void init() {
		try {
			loadLookupTable();
			log.info("Loaded relations lookup table from {}, total entries: {}", RELATIONS_CSV_RESOURCE_PATH,
					lookupTable.size());
		}
		catch (Exception e) {
			log.error("Failed to load Lookup table: {}", e.getMessage());
			e.printStackTrace();
		}
	}

	/**
	 * Load the lookup tables from resources and store them in memory for easy retrieval
	 *
	 * Loads the lookup table from RELATIONS_CSV_RESOURCE_PATH
	 * @throws IOException exception when reading or parsing the file
	 */
	public void loadLookupTable() throws IOException {
		String data = Helpers.readFileOnClassPath(RELATIONS_CSV_RESOURCE_PATH);

		HeaderColumnNameMappingStrategy<RelationLookupEntry> strategy = new HeaderColumnNameMappingStrategy<>();
		strategy.setType(RelationLookupEntry.class);

		CsvToBean<RelationLookupEntry> csvToBean = new CsvToBeanBuilder<RelationLookupEntry>(new StringReader(data))
				.withMappingStrategy(strategy).withIgnoreLeadingWhiteSpace(true).build();

		List<RelationLookupEntry> lookupEntries = csvToBean.parse();
		lookupEntries.forEach(entry -> lookupTable.put(entry.getRelationType(), entry));
	}

	/**
	 * Obtain the generated look up table
	 * @return lookupTable
	 */
	public Map<String, RelationLookupEntry> getLookupTable() {
		return lookupTable;
	}

	/**
	 * Check if a relationType is included in the lookup table
	 * @param relationType the relationType to check
	 * @return true|false whether the relationType is included
	 */
	public boolean contains(String relationType) {
		return lookupTable.containsKey(relationType);
	}

	/**
	 * Resolves a relationType to it's corresponding {@link RelationLookupEntry}
	 * @param relationType the String relationType
	 * @return the {@link RelationLookupEntry} from the lookup table
	 */
	public RelationLookupEntry resolve(String relationType) {
		if (lookupTable.containsKey(relationType)) {
			return lookupTable.get(relationType);
		}
		return null;
	}

	/**
	 * Quick method to obtain the reverse form of a relationType
	 * @param relationType the relationType
	 * @return the reverse String of the relationType
	 */
	public String getReverse(String relationType) {
		if (lookupTable.containsKey(relationType)) {
			RelationLookupEntry entry = lookupTable.get(relationType);
			return entry.getReverseRelationType();
		}
		return null;
	}

}