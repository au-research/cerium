package ardc.cerium.core.common.repository.specs;

import ardc.cerium.core.common.entity.Version;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.List;

public class VersionSpecification extends SearchSpecification implements Specification<Version> {

	@Override
	public Predicate toPredicate(Root<Version> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) {
		List<Predicate> predicates = super.getBasicPredicate(root, criteriaQuery, criteriaBuilder);
		return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
	}

}
