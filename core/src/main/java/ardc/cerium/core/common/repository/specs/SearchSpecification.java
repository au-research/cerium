package ardc.cerium.core.common.repository.specs;

import javax.persistence.criteria.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class SearchSpecification {

	public List<SearchCriteria> list;

	public SearchSpecification() {
		this.list = new ArrayList<>();
	}

	public void add(SearchCriteria criteria) {
		this.list.add(criteria);
	}

	public List<Predicate> getBasicPredicate(Root<?> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder builder) {
		List<Predicate> predicates = new ArrayList<>();

		for (SearchCriteria criteria : list) {
			if (criteria.getOperation().equals(SearchOperation.GREATER_THAN)) {
				predicates.add(builder.greaterThan(root.get(criteria.getKey()), criteria.getValue().toString()));
			}
			else if (criteria.getOperation().equals(SearchOperation.LESS_THAN)) {
				predicates.add(builder.lessThan(root.get(criteria.getKey()), criteria.getValue().toString()));
			}
			else if (criteria.getOperation().equals(SearchOperation.GREATER_THAN_EQUAL)) {
				predicates
						.add(builder.greaterThanOrEqualTo(root.get(criteria.getKey()), criteria.getValue().toString()));
			}
			else if (criteria.getOperation().equals(SearchOperation.DATE_GREATER_THAN_EQUAL)) {
				// special join to the record table
				predicates.add(builder.greaterThanOrEqualTo(root.join("record").get(criteria.getKey()),
						(Date) criteria.getValue()));
			}
			else if (criteria.getOperation().equals(SearchOperation.DATE_LESS_THAN_EQUAL)) {
				// special join to the record table
				predicates.add(builder.lessThanOrEqualTo(root.join("record").get(criteria.getKey()),
						(Date) criteria.getValue()));
			}
			else if (criteria.getOperation().equals(SearchOperation.LESS_THAN_EQUAL)) {
				predicates.add(builder.lessThanOrEqualTo(root.get(criteria.getKey()), criteria.getValue().toString()));
			}
			else if (criteria.getOperation().equals(SearchOperation.NOT_EQUAL)) {
				predicates.add(builder.notEqual(root.get(criteria.getKey()), criteria.getValue()));
			}
			else if (criteria.getOperation().equals(SearchOperation.EQUAL)) {
				predicates.add(builder.equal(root.get(criteria.getKey()), criteria.getValue()));
			}
			else if (criteria.getOperation().equals(SearchOperation.MATCH)) {
				predicates.add(builder.like(builder.lower(root.get(criteria.getKey())),
						"%" + criteria.getValue().toString().toLowerCase() + "%"));
			}
			else if (criteria.getOperation().equals(SearchOperation.MATCH_END)) {
				predicates.add(builder.like(builder.lower(root.get(criteria.getKey())),
						criteria.getValue().toString().toLowerCase() + "%"));
			}
			else if (criteria.getOperation().equals(SearchOperation.MATCH_START)) {
				predicates.add(builder.like(builder.lower(root.get(criteria.getKey())),
						"%" + criteria.getValue().toString().toLowerCase()));
			}
			else if (criteria.getOperation().equals(SearchOperation.IN)) {
				predicates.add(builder.in(root.get(criteria.getKey())).value(criteria.getValue()));
			}
			else if (criteria.getOperation().equals(SearchOperation.NOT_IN)) {
				predicates.add(builder.not(root.get(criteria.getKey())).in(criteria.getValue()));
			}
			else if (criteria.getOperation().equals(SearchOperation.RECORD_EQUAL)) {
				// special join to the record table
				predicates.add(builder.equal(root.join("record").get(criteria.getKey()), criteria.getValue()));
			}
			else if (criteria.getOperation().equals(SearchOperation.RECORD_IN)) {
				// special join to the record table
				predicates.add(builder.in(root.join("record").get(criteria.getKey())).value(criteria.getValue()));
			}
			else if (criteria.getOperation().equals(SearchOperation.IDENTIFIER_EQUAL)) {
				// special join to the record table
				predicates.add(builder.equal(root.join("identifiers").get(criteria.getKey()), criteria.getValue()));
			}
		}

		return predicates;
	}

}
