package com.aktimetrix.service.planner.util;

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
