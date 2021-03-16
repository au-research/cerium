package ardc.cerium.drvs.model;

import com.opencsv.bean.CsvBindByName;
import com.opencsv.bean.CsvBindByPosition;

public class DRVSSubmission {

	@CsvBindByName(column = "Local Collection ID")
	private String localCollectionID;

	@CsvBindByName(column = "Collection Capacity")
	private String collectionCapacity;

	@CsvBindByName(column = "Application Research Disciplines")
	private String applicationResearchDisciplines;

	@CsvBindByName(column = "Title")
	private String title;

	@CsvBindByName(column = "Description")
	private String description;

	@CsvBindByName(column = "Data Controller/s")
	private String DataControllers;

	@CsvBindByName(column = "DOI")
	private String DOI;

	public String getLocalCollectionID() {
		return localCollectionID;
	}

	public void setLocalCollectionID(String localCollectionID) {
		this.localCollectionID = localCollectionID.trim();
	}

	public String getCollectionCapacity() {
		return collectionCapacity;
	}

	public void setCollectionCapacity(String collectionCapacity) {
		this.collectionCapacity = collectionCapacity;
	}

	public String getApplicationResearchDisciplines() {
		return applicationResearchDisciplines;
	}

	public void setApplicationResearchDisciplines(String applicationResearchDisciplines) {
		this.applicationResearchDisciplines = applicationResearchDisciplines;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getDataControllers() {
		return DataControllers;
	}

	public void setDataControllers(String dataControllers) {
		DataControllers = dataControllers;
	}

	public String getDOI() {
		return DOI;
	}

	public void setDOI(String DOI) {
		this.DOI = DOI;
	}
}
