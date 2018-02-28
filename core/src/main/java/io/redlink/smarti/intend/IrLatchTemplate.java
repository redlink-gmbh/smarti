/*
 * Copyright 2017 Redlink GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package io.redlink.smarti.intend;

import io.redlink.smarti.api.QueryBuilder;
import io.redlink.smarti.model.Slot;
import io.redlink.smarti.model.TemplateDefinition;
import io.redlink.smarti.model.Token;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;

/**
 * QueryTemplate for LATCH information retrieval. This template defines roles for
 * the five dimensions of LATCH:<ol>
 * <li>{@link #ROLE_LOCATION <b>L</b>ocation}: defines the spatial context of the search
 * <li>{@link #ROLE_ALPHABET <b>A</b>lphabet}: the full text context for the search
 * <li>{@link #ROLE_TIME <b>T</b>ime}: defines the temporal context of the search
 * <li>{@link #ROLE_CATEGORY <b>C</b>ategory}: categorizations of any kinds
 * <li>{@link #ROLE_HIERARCHY <b>H</b>ierarchy}: things like price ranges, 1-5 stars, ratings ...
 * </ol>
 * 
 * {@link QueryBuilder} my use some or all of those templates. In addition they might want
 * to use values of {@link Token}s assigned to roles different as {@link #ROLE_ALPHABET alphabet}
 * for full text queries (e.g. the name of a location token).
 * 
 * @author Rupert Westenthaler
 *
 */
@Component
public class IrLatchTemplate extends TemplateDefinition {

    /**
     * The Information Retrieval based on LATCH
     */
    public static final String IR_LATCH = "ir_latch";
    
    public static final String ROLE_LOCATION = "location";
    public static final String ROLE_ALPHABET = "alphabet";
    public static final String ROLE_TIME = "time";
    public static final String ROLE_CATEGORY = "category";
    /**
     * Things like price ranges, Rating ranges ...
     */
    public static final String ROLE_HIERARCHY = "hierarchy";
    
    public IrLatchTemplate() {
        super(IR_LATCH);
    }

    @Override
    protected Slot createSlotForName(String name) {
        switch (name) {
        case ROLE_LOCATION:
            return new Slot(ROLE_LOCATION, Token.Type.Place);
        case ROLE_ALPHABET:
            return new Slot(ROLE_ALPHABET, null);
        case ROLE_TIME:
            return new Slot(ROLE_TIME, Token.Type.Date);
        case ROLE_CATEGORY:
            return new Slot(ROLE_CATEGORY, Token.Type.Topic);
        case ROLE_HIERARCHY:
            return new Slot(ROLE_HIERARCHY, null);
        default:
            log.warn("Unknown QuerySlot '{}' requested for {}", name, getClass().getSimpleName());
            return null;
        }
    }

    @Override
    protected boolean validate(Collection<Slot> slots, List<Token> tokens) {
        return true; //#200: with do now want templates without slots to be valid so that queries are included
//        return slots.stream()
//                .filter(s -> s.getTokenIndex() >= 0).findAny().isPresent();
    }

}
