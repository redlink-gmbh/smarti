/*
 * Copyright (c) 2017 Redlink GmbH.
 */
package io.redlink.smarti.webservice.pojo;

import io.redlink.smarti.model.Slot;
import io.redlink.smarti.model.Token;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by jakob on 29.06.17.
 */
public class QueryUpdate {

    private List<Token> tokens = new LinkedList<>();

    private Collection<Slot> slots = new LinkedList<>();


    public List<Token> getTokens() {
        return tokens;
    }

    public void setTokens(List<Token> tokens) {
        this.tokens = tokens;
    }

    public Collection<Slot> getSlots() {
        return slots;
    }

    public void setSlots(Collection<Slot> slots) {
        this.slots = slots;
    }
}
