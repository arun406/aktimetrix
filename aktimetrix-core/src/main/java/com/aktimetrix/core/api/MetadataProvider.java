package com.aktimetrix.core.api;

import java.util.Map;

public interface MetadataProvider<T> {

    /**
     * prepare metadata
     *
     * @return metadata
     */
    Map<String, Object> getMetadata(T data);
}
