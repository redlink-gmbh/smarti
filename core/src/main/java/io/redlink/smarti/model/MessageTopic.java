/*
 * Copyright (c) 2016 Redlink GmbH
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
