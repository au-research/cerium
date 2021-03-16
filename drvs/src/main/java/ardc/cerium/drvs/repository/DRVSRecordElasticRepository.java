package ardc.cerium.drvs.repository;

import ardc.cerium.drvs.dto.DRVSRecordDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface DRVSRecordElasticRepository extends ElasticsearchRepository<DRVSRecordDocument, String> {

}
