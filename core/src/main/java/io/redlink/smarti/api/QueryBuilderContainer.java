/*
 * Copyright (c) 2017 Redlink GmbH.
 */
package io.redlink.smarti.api;

import java.util.Set;

/**
 * A Container for programmatically created QueryBuilders.
 */
public interface QueryBuilderContainer {

    Set<QueryBuilder> getQueryBuilders();

}

