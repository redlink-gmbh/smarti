package io.redlink.nlp.model.dep;

import io.redlink.nlp.model.tag.TagSet;

public class UniversalDependencies {

    private UniversalDependencies(){
        throw new UnsupportedOperationException("Do not use reflections to create instances of this class :(");
    }
    
    /**
     * The {@link TagSet} for the <a href="http://universaldependencies.org/u/dep/index.html">Universal Dependencies</a>
     */
    public static final TagSet<RelTag> REL_TAG_SET = new TagSet<RelTag>("Universal Dependencies Tagset");

    static {
        REL_TAG_SET.addTag(new RelTag("acl", GrammaticalRelation.AdjectivalClause)); //NOTE: added to GrammaticalRelation
        REL_TAG_SET.addTag(new RelTag("advcl", GrammaticalRelation.AdverbialClauseModifier));
        REL_TAG_SET.addTag(new RelTag("advmod", GrammaticalRelation.AdverbialModifier));
        REL_TAG_SET.addTag(new RelTag("amod", GrammaticalRelation.AdjectivalModifier));
        REL_TAG_SET.addTag(new RelTag("appos", GrammaticalRelation.AppositionalModifier));
        REL_TAG_SET.addTag(new RelTag("aux", GrammaticalRelation.Auxiliary));
        REL_TAG_SET.addTag(new RelTag("auxpass", GrammaticalRelation.PassiveAuxiliary));
        REL_TAG_SET.addTag(new RelTag("case")); //TODO: case marking
        REL_TAG_SET.addTag(new RelTag("cc", GrammaticalRelation.CoordinatingConjunction));
        REL_TAG_SET.addTag(new RelTag("ccomp", GrammaticalRelation.ClausalComplement));
        REL_TAG_SET.addTag(new RelTag("compound", GrammaticalRelation.Compound));
        REL_TAG_SET.addTag(new RelTag("conj", GrammaticalRelation.Conjunct));
        REL_TAG_SET.addTag(new RelTag("cop", GrammaticalRelation.Copula));
        REL_TAG_SET.addTag(new RelTag("csubj", GrammaticalRelation.ClausalSubject));
        REL_TAG_SET.addTag(new RelTag("csubjpass", GrammaticalRelation.ClausalPassiveSubject));
        REL_TAG_SET.addTag(new RelTag("dep", GrammaticalRelation.UnspecifiedDependency));
        REL_TAG_SET.addTag(new RelTag("det", GrammaticalRelation.Determiner));
        REL_TAG_SET.addTag(new RelTag("discourse", GrammaticalRelation.DiscourseElement));
        REL_TAG_SET.addTag(new RelTag("dislocated", GrammaticalRelation.DislocatedElement)); 
        REL_TAG_SET.addTag(new RelTag("dobj", GrammaticalRelation.DirectObject));
        REL_TAG_SET.addTag(new RelTag("expl", GrammaticalRelation.Expletive));
        REL_TAG_SET.addTag(new RelTag("foreign",GrammaticalRelation.Foreign));
        REL_TAG_SET.addTag(new RelTag("goeswith", GrammaticalRelation.GoesWith));
        REL_TAG_SET.addTag(new RelTag("iobj", GrammaticalRelation.IndirectObject));
        REL_TAG_SET.addTag(new RelTag("list", GrammaticalRelation.List));
        REL_TAG_SET.addTag(new RelTag("mark", GrammaticalRelation.Marker));
        REL_TAG_SET.addTag(new RelTag("mwe", GrammaticalRelation.MultiWordExpression));
        REL_TAG_SET.addTag(new RelTag("name", GrammaticalRelation.Name));
        REL_TAG_SET.addTag(new RelTag("neg", GrammaticalRelation.NegationModifier));
        REL_TAG_SET.addTag(new RelTag("nmod", GrammaticalRelation.NominalModifier));
        REL_TAG_SET.addTag(new RelTag("nsubj", GrammaticalRelation.NominalSubject));
        REL_TAG_SET.addTag(new RelTag("nsubjpass", GrammaticalRelation.PassiveNominalSubject));
        REL_TAG_SET.addTag(new RelTag("nummod", GrammaticalRelation.NumericModifier));
        REL_TAG_SET.addTag(new RelTag("parataxis", GrammaticalRelation.Parataxis));
        REL_TAG_SET.addTag(new RelTag("punct", GrammaticalRelation.Punctuation));
        REL_TAG_SET.addTag(new RelTag("remnant", GrammaticalRelation.Remnant));
        REL_TAG_SET.addTag(new RelTag("reparandum", GrammaticalRelation.Reparandum));
        REL_TAG_SET.addTag(new RelTag("root", GrammaticalRelation.SentenceHead));
        REL_TAG_SET.addTag(new RelTag("vocative", GrammaticalRelation.Vocative));
        REL_TAG_SET.addTag(new RelTag("xcomp", GrammaticalRelation.OpenClausalComplement));
    }
    
}
