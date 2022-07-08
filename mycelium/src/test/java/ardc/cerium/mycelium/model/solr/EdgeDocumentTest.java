package ardc.cerium.mycelium.model.solr;

import ardc.cerium.mycelium.model.Vertex;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EdgeDocumentTest {

	@Test
	void equals() {
		EdgeDocument a = new EdgeDocument("isPartOf");
		a.setRelationOrigin("GrantsNetwork");

		EdgeDocument b = new EdgeDocument("isPartOf");
		b.setRelationOrigin("GrantsNetwork");

		assertEquals(a, b);
		assertTrue(List.of(a).contains(b));

		Vertex v = new Vertex();
		assertFalse(a.equals(v));
	}

	@Test
	void equals_not_origin() {
		EdgeDocument a = new EdgeDocument("isPartOf");
		a.setRelationOrigin("GrantsNetwork");

		EdgeDocument b = new EdgeDocument("isPartOf");
		b.setRelationOrigin("RelatedObject");

		assertNotEquals(a, b);

		assertFalse(List.of(a).contains(b));
	}

	@Test
	void equals_not_type() {
		EdgeDocument a = new EdgeDocument("isPartOf");

		EdgeDocument b = new EdgeDocument("hasPart");

		assertNotEquals(a, b);

		assertFalse(List.of(a).contains(b));
	}

}