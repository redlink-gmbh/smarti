/*
* Licensed to the Apache Software Foundation (ASF) under one or more
* contributor license agreements.  See the NOTICE file distributed with
* this work for additional information regarding copyright ownership.
* The ASF licenses this file to You under the Apache License, Version 2.0
* (the "License"); you may not use this file except in compliance with
* the License.  You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package io.redlink.nlp.model.dep;

import io.redlink.nlp.model.morpho.Tense;

import java.util.*;

/**
 * Enumeration over all grammatical relation as defined by the
 * <a href="http://universaldependencies.org/u/dep/index.html">Universal Dependencies<a/>
 * 
 * @author Rupert Westenthaler
 * 
 */
public enum GrammaticalRelation {

    NominalDependency("Nominal Dependencies"),
    PredicateDependency("Predicate Dependencies"),
    ModifierWord("Modifier Word"),
    AuxiliaryDependnecy,
    SentenceHead("Sentence head"),
    UnspecifiedDependency("Unspecified dependency"),
    CoreDependentsOfCausalPredicates("Core dependents of clausal predicates"),
    NominalSubject("Nominal Subject",CoreDependentsOfCausalPredicates,NominalDependency),
    ClausalSubject("Clausal Subject",CoreDependentsOfCausalPredicates,PredicateDependency),
    PassiveNominalSubject("Passive Nominal Subject",CoreDependentsOfCausalPredicates,NominalDependency),
    ClausalPassiveSubject("Clausal Passive Subject", CoreDependentsOfCausalPredicates,PredicateDependency),
    DirectObject("Direct Object",CoreDependentsOfCausalPredicates,NominalDependency),
    ClausalComplement("Clausal Complement",CoreDependentsOfCausalPredicates,PredicateDependency),
    OpenClausalComplement("Open Clausal Complement", CoreDependentsOfCausalPredicates),
    IndirectObject("Indirect Object", CoreDependentsOfCausalPredicates,NominalDependency),
    NounDependents("Noun dependents"),
    NumericModifier("Numeric Modifier",NounDependents,NominalDependency),
    AdjectivalClause("Clausal Modifier of Noun (Adjectival Clause)",NounDependents,PredicateDependency),
    AdjectivalModifier("Adjectival Modifier",NounDependents,ModifierWord),
    AppositionalModifier("Appositional Modifier",NounDependents,NominalDependency),
    Determiner(NounDependents,ModifierWord),
    NonCoreDependentsOfClausalPredicates("Non-core dependents of clausal predicates"),
    NominalModifier("Nominal Modifier",NounDependents,NonCoreDependentsOfClausalPredicates,NominalDependency),
    NegationModifier("Negation Modifier",NounDependents,NonCoreDependentsOfClausalPredicates,ModifierWord),
    AdverbialClauseModifier("Adverbial Clause Modifier",NonCoreDependentsOfClausalPredicates,PredicateDependency),
    AdverbialModifier("Adverbial Modifier",NonCoreDependentsOfClausalPredicates,ModifierWord),
    CompoundingAndUnanalyzed("Compounding and Unanalyzed"),
    Compound(CompoundingAndUnanalyzed),
    MultiWordExpression("Multi-Word Expression",CompoundingAndUnanalyzed),
    GoesWith("Goes With",CompoundingAndUnanalyzed),
    Name(CompoundingAndUnanalyzed),
    Foreign(CompoundingAndUnanalyzed),
    LooseJoiningRelations("Loose Joining Relations"),
    List(LooseJoiningRelations),
    Parataxis(LooseJoiningRelations),
    Remnant("Remnant in Ellipsis",LooseJoiningRelations),
    DislocatedElement("Dislocated Elements",LooseJoiningRelations),
    Reparandum("Reparandum (overridden disfluency)",LooseJoiningRelations),
    SpecialClausalDependents("Special clausal dependents"),
    Vocative(SpecialClausalDependents,NominalDependency),
    Auxiliary(SpecialClausalDependents,AuxiliaryDependnecy),
    Marker("Marker",SpecialClausalDependents),
    DiscourseElement("Discourse Element",SpecialClausalDependents,NominalDependency),
    PassiveAuxiliary("Passive Auxiliary",SpecialClausalDependents,AuxiliaryDependnecy),
    Expletive(SpecialClausalDependents,NominalDependency),
    Copula(SpecialClausalDependents,AuxiliaryDependnecy),
    Coordination,
    Punctuation(SpecialClausalDependents,Coordination),
    Conjunct(Coordination),
    CoordinatingConjunction("coordinating conjunction",Coordination),
    ;
    
	/**
	 * The parents of this grammatical relation.
	 */
	private Collection<GrammaticalRelation> parents;

    String desc;
    
    GrammaticalRelation(GrammaticalRelation...parents) {
        this(null, parents);
    }
	GrammaticalRelation(String desc, GrammaticalRelation...parents) {
	    this.desc = desc;
	    this.parents = parents == null || parents.length <= 0 ? Collections.emptySet() : Arrays.asList(parents); 
	}

	public final String description(){
	    return desc == null ? name() : desc;
	}
	
	public Collection<GrammaticalRelation> parents() {
		return this.parents;
	}
	
    public Set<GrammaticalRelation> hierarchy() {
        return transitiveClosureMap.get(this);
    }

    @Override
    public String toString() {
        return "up:" + name();
    }
    
    /**
     * This is needed because one can not create EnumSet instances before the
     * initialization of an Enum has finished.<p>
     * To keep using the much faster {@link EnumSet} a static member initialized
     * in an static {} block is used as a workaround. The {@link Tense#getTenses()}
     * method does use this static member instead of a member variable
     */
    private static final Map<GrammaticalRelation,Set<GrammaticalRelation>> transitiveClosureMap;
    
    static {
        transitiveClosureMap = new EnumMap<GrammaticalRelation,Set<GrammaticalRelation>>(GrammaticalRelation.class);
        for(GrammaticalRelation pos : GrammaticalRelation.values()){
            Set<GrammaticalRelation> parents = EnumSet.of(pos);
            for(GrammaticalRelation posParent : pos.parents()){
                Set<GrammaticalRelation> transParents = transitiveClosureMap.get(posParent);
                if(transParents != null){
                    parents.addAll(transParents);
                } else if(posParent != null){
                    parents.add(posParent);
                } // else no parent
            }
            transitiveClosureMap.put(pos, parents);
        }
    }
	
}
