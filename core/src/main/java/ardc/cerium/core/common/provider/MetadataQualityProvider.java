package ardc.cerium.core.common.provider;

import ardc.cerium.core.common.entity.Identifier;

import java.util.List;

public interface MetadataQualityProvider {
    String get(String content, String localIdentifierValue);
}