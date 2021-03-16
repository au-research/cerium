package ardc.cerium.core.common.repository.specs;

import ardc.cerium.core.common.entity.Record;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.List;

public class RecordSpecification extends SearchSpecification implements Specification<Record> {

	@Override
	public Predicate toPredicate(Root<Record> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder builder) {
		List<Predicate> predicates = super.getBasicPredicate(root, criteriaQuery, builder);
		return builder.and(predicates.toArray(new Predicate[0]));
	}

}
