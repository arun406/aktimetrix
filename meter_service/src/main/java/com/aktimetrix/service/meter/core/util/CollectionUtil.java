package com.aktimetrix.service.meter.core.util;

import java.util.Collection;

/**
 *
 */
public class CollectionUtil {

    public static boolean isEmptyOrNull(Collection<?> collection) {
        return (collection == null || collection.isEmpty());
    }

    public static boolean isNotEmptyOrNull(Collection<?> collection) {
        return !isEmptyOrNull(collection);
    }


}
