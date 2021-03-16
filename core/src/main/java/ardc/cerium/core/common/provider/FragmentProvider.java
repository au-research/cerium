package ardc.cerium.core.common.provider;

import java.util.List;

public interface FragmentProvider {

	String get(String content, int position);

	int getCount(String content);

}
