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
package io.redlink.smarti.model;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * The topic of the message 
 */
public enum MessageTopic {

    Reiseplanung,
    ReiseplanungBahn(Reiseplanung),
    ReiseplanungExtern(Reiseplanung),
    ReiseplanungLokal(Reiseplanung),
    Umkreissuche,
    UmkreissucheUnterkunft(Umkreissuche),
    UmkreissucheGastronomie(Umkreissuche),
    UmkreissucheWasTun(Umkreissuche),
    UmkreissucheEinkaufenService(Umkreissuche),
    UmkreissucheDbBahnProdukt(Umkreissuche),
    Danke,
    Reisebuddy,
    Produkt,
    DBProdukt(Produkt),
    Zuginformation,
    Sonstiges,
    /* Hasso */
    ApplicationHelp,
    ;
    
    MessageTopic parent;
    
    MessageTopic(){
        this(null);
    }
    MessageTopic(MessageTopic parent){
        this.parent = parent;
    }
    /**
     * The direct parent
     */
    public MessageTopic parent() {
        return parent;
    }

    /**
     * Transitive Closure over the parents
     */
    public Set<MessageTopic> hierarchy() {
        return transitiveClosureMap.get(this);
    }

    
    /**
     * This is needed because one can not create EnumSet instances before the
     * initialization of an Enum has finished.<p>
     * To keep using the much faster {@link EnumSet} a static member initialized
     * in an static {} block is used as a workaround. The {@link #hierarchy()}
     * method does use this static member instead of a member variable
     */
    private static final Map<MessageTopic,Set<MessageTopic>> transitiveClosureMap;
    
    static {
        transitiveClosureMap = new EnumMap<>(MessageTopic.class);
        for(MessageTopic topic : MessageTopic.values()){
            Set<MessageTopic> parents = EnumSet.of(topic);
            MessageTopic parent = topic.parent();
            if(parent != null){
                Set<MessageTopic> transParents = transitiveClosureMap.get(parent);
                if(transParents != null){
                    parents.addAll(transParents);
                } else {
                    parents.add(parent);
                } // else no parent
            }
            transitiveClosureMap.put(topic, parents);
        }
    }
}
