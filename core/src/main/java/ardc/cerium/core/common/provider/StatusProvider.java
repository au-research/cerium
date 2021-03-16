package ardc.cerium.core.common.provider;

import ardc.cerium.core.common.entity.Record;

public interface StatusProvider {
    String get(Record record);
}
