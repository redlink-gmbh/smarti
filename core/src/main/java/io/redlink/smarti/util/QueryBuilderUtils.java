/*
 * Copyright (c) 2016 Redlink GmbH
 */
package io.redlink.smarti.util;

import io.redlink.smarti.api.QueryBuilder;

import static org.apache.commons.lang3.StringUtils.uncapitalize;

/**
 */
public class QueryBuilderUtils {

    private QueryBuilderUtils() {
        throw new AssertionError("No io.redlink.reisebuddy.util.QueryBuilderUtils instances for you!");
    }

    public static String getQueryBuilderName(Class<? extends QueryBuilder> aClass) {
        return uncapitalize(aClass.getSimpleName().replaceFirst("(?i:(query)?(builder)$)", "").replaceAll("(?i:[^a-z0-9]+)", ""));
    }

}
