package ardc.cerium.mycelium.util;

import ardc.cerium.mycelium.rifcs.model.datasource.DataSource;
import ardc.cerium.mycelium.rifcs.model.datasource.settings.primarykey.PrimaryKey;

import java.util.List;
import java.util.stream.Collectors;

public class DataSourceUtil {

    public static List<PrimaryKey> getPrimaryKeyDifferences(DataSource from, DataSource to) {
        return to.getPrimaryKeySetting().getPrimaryKeys().stream()
                .filter(pk -> !from.getPrimaryKeySetting().getPrimaryKeys().contains(pk))
                .collect(Collectors.toList());
    }
}
