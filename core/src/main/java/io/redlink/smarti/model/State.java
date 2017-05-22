/*
 * Copyright (c) 2016 Redlink GmbH
 */
package io.redlink.smarti.model;

/**
 * The state of the Token. Intended to be used for
 * acquiring user feedback (e.g. for improving the training sets for
 * machine learning models)
 */
public enum State {
    /**
     * Initial state for extracted tokens
     */
    Suggested,
    /**
     * State for tokens that where confirmed (e.g. used in a query)
     */
    Confirmed,
    /**
     * State for tokens that where rejected (e.g. removed from a
     * proposed query)
     */
    Rejected
}
