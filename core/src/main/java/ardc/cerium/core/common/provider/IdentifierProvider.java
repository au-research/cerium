package ardc.cerium.core.common.provider;

import ardc.cerium.core.common.model.Schema;

import java.util.List;

public interface IdentifierProvider {

	String get(String content);

	String get(String content, int position);

	List<String> getAll(String content);

	void setPrefix(String prefix);

}
