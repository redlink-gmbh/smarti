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
package io.redlink.nlp.model.phrase;

import io.redlink.nlp.model.morpho.Tense;
import io.redlink.nlp.model.pos.LexicalCategory;

import java.util.*;


/**
 * Categories for Phrases based on the <a href="http://olia.nlp2rdf.org/">Olia</a> Ontology.
 * All elements defined in the enumeration are sub-classes of 
 * <a href="http://purl.org/olia/olia.owl#Phrase">olia:Phrase</a> however it also
 * includes an entry for <a href="http://purl.org/olia/olia.owl#Sentence">olia:Sentence</a>
 */
public enum PhraseCategory {
    
    Phrase,
    /**
     * An adjective phrase may consist of an adjective, or a sequence of words in which an adjective is the head of the
     * phrase, as shown in 47 to 50 below.
     * <pre>
     *      (47)    [NP his [ADJP surprisingly thick and hairy ADJP] wrists NP]
     *      (48)    [NP some [ADJP [ADJP wholly unanticipated ADJP] but [ADJP remotely possible ADJP] ADJP] event NP]
     *      (49)    [S [NP His speeches NP] [VP are [ADVP always ADVP] [ADJP too long [PP for comfort PP] ADJP] VP] S]
     *      (50)    [AUX have AUX] [NP you NP] [VP found [NP something 
     *              [ADJP suitable [PP for [NP your needs NP] PP] ADJP] NP] VP] ?
     *      (<a href="http://www.ilc.cnr.it/EAGLES96/segsasg1/node36.html">more Information</a>)
     *</pre>
     */
    AdjectivePhrase(LexicalCategory.Adjective, Phrase),
    /**
     * An adverb phrase may consist of an adverb, or a sequence of words in which an adverb is the head of the phrase.
     * Adverb phrases may function as adverbials, as in 41:
     * <pre>
     * (41)      [NP Her beautiful white hat NP] [VP was [ADVP very nearly ADVP] ruined VP]
     * </pre>
     * or as modifiers of adjectives, as in 42:
     * <pre>
     * (42)      [NP Il NP] [VP parle [ADVP infiniment plus couramment ADVP] VP]
     * </pre>
     * or noun phrases, as in 43:
     * <pre>
     * (43)      [NP They NP] [VP let [NP me NP] [VP speak VP] [ADVP now and then ADVP] VP]
     * </pre>
     * or as the complement of a preposition, as in 44:
     * <pre>
     * (44)      [ADVP Strangely enough ADVP] , [NP we NP] [VP received [NP a reply NP] [NP the next day NP] VP]
     * </pre>
     * Other examples:
     * <pre>
     * (45)      [NP The book NP] [VP is [ADVP right here ADVP] VP]
     * (46)      [ADVP Como [NP resultado [PP de [NP esa trama NP] PP] NP] ADVP] 
     *           [VP no se lleva [PP a cabo PP] [NP ninguna acción NP] VP] 
     * </pre>
     * (<a href="http://www.ilc.cnr.it/EAGLES96/segsasg1/node35.html">more Informaton</a>)
     */
    AdverbPhrase(LexicalCategory.Adverb, Phrase),
    /**
     * Multi-word conjunction
     * <p>  
     * Besides the usual and, or, but, etc., certain prepositions and subordinating conjunctions can be used as coordinating conjunctions. Multi-word coordinating conjunctions are labeled CONJP (see section 7 [Coordination]). ...
     * <p>
     * CONJP — Conjunction Phrase. Used to mark certain “multi-word” conjunctions, such as as well as, instead of. (Bies et al. 1995)
     */
    ConjunctionPhrase(LexicalCategory.Conjuction, Phrase),
    /**
     * As has already been shown in some of the preceding examples, the issue of coordination necessarily arises: how is
     * coordination to be represented in terms of constituency? Different approaches have been taken, and in the example
     * analyses given in this document, we have chosen to take a traditional approach, showing the coordinated constituents
     * at the same level, with the conjunction between them (see also 47 and 48):
     * <pre>
     * (51)     [NP [NP John NP] and [NP Mary NP] NP]
     * (52)     She went [PP [PP to the library PP] or [PP to the cafeteria PP] PP]
     * (53)     He works [ADVP [ADVP very slowly ADVP] but [ADVP very meticulously ADVP] ADVP]
     * </pre>
     * However, in practice, in an automated parsing system, this is not an easy differentiation to make, and in some
     * existing schemes, a slightly less satisfactory solution has been found, viz. analysing coordination in a similar
     * fashion to subordination.
     * <p>
     * Most constituents (both phrases and clauses) can be coordinated, but the extent to which this is possible will
     * differ across languages. The conjuncts may be marked as such by separate descriptors: NPtex2html_wrap_inline4084
     * etc. However, there are many occasions where the conjuncts are not of the same formal category, or where they do not
     * correspond to an entire phrasal or clausal constituent. There is much to be said, in these cases, or perhaps for all
     * cases of coordination, for the use of a generalised label applied to all coordinate constituents or conjuncts, e.g.
     * the label CO used in the TOSCA system. We do not offer a definitive solution for the annotation of coordination, and
     * the many variants of coordination will not be considered further in this report. See Sampson (1995: 310f) for a
     * detailed treatment. <p>
     * (<a href="http://www.ilc.cnr.it/EAGLES96/segsasg1/node37.html">more Information</a>)     */
    Coordination(Phrase),
    /**
     * Certain pronouns serving as determiners in noun phrases may be premodified, for instance, 
     * by degree adverbs such as in German "so viele ¨Altere", "gar kein Schutz", etc.
     * In the case of "so viele Ältere", the premodifying adverb so is attached to the indefinite
     * pronoun viele. Together, they form a determiner phrase (DP), which is attached to the
     * head noun Ältere on the same level: [so viele] Ältere<br>
     * (Telljohann et al. 2009, p.63)
     */
    DeterminerPhrase(LexicalCategory.PronounOrDeterminer, Phrase),
    /**
     * Single foreign words are projected to a syntactic level assigned the node label FX, which is
     * an universal label for any syntactic category (phrasal and sentential) in the respective
     * foreign language.<br>
     * (Telljohann et al. 2009, p.44)
     */
    ForeignPhrase(Phrase),
    /**
     * A NounHeadedPhrase takes a nominal as its (semantic) head. 
     * Introduced as a generalization over NounPhrase and PrepositionalPhrase for reasons of consistency with dependency parsers like 
     * Connexor where this differentiation is not made.
     */
    NounHeadedPhrase(LexicalCategory.Noun, Phrase),
    /**

     * At phrase level, the noun phrase is probably the least problematic of the categories to be dealt 
     * with. In general, a noun phrase will a have noun or a pronoun as its head, and included within 
     * the noun phrase are the determinative elements, any premodification, and any postmodification. 
     * The examples below, 14 to 17 show noun phrases with the head noun/pronoun in bold:
     * <pre>
     * (14)     [NP He NP] was a tiny man
     * (15)     [NP his white shirt cuffs NP]
     * (16)     [NP his surprisingly thick and hairy wrists NP]
     * (17)     [NP some wholly unanticipated but remotely possible event of absorbing interest NP]
     * </pre>
     * However, noun phrases may also occur with adjectival heads, as in 18 and 19:
     * <pre>
     * (18)     [NP The unemployed NP] have had enough
     * (19)     We've beaten [NP the best NP]
     * </pre>
     * or with a head which is a cardinal or ordinal number, as in 20 and 21:
     * <pre>
     * (20)     [NP The ninth NP] is my particular favourite
     * (21)     [NP The other seven NP] continued with the trip
     * </pre>
     * In `pro-drop' languages, such as Spanish and Italian, pronominal Subjects are usually not expressed. 
     * Depending on the chosen type of analysis, this may require another definition of noun phrase, in order 
     * to include `empty noun phrases', in which the pronoun is not actually present, but may be inferred 
     * from the verb ending.
     * <p>
     * A classic constituency test for Noun Phrases is that only whole NPs can be moved within the same sentence. 
     * In English, constituents can be preposed to achieve some effect, as in 23 (from Radford 1988: 70):
     * <pre>
     * (22)     I can't stand your elder sister
     * (23)     Your elder sister I can't stand (though your brother's OK).
     * </pre>
     * Examples 24 and 25 show that it is not possible to move only part of the NP:
     * <pre>
     * (24)     *Your elder I can't stand sister
     * (25)     *Elder sister, I can't stand your
     * </pre>
     * However, this test should be used with caution. It works well in English, but not always in other languages. 
     * For example, in 26 Neue Bücher is moved to the beginning of the sentence while keine is left at the end:
     * <pre>
     * (26)     Neue    Bücher  habe    ich keine
     *          new books   have    I   no
     *          `I have not got any new books'
     * </pre>
     * (<a href="http://www.ilc.cnr.it/EAGLES96/segsasg1/node32.html">more information</a>)
     */
    NounPhrase(NounHeadedPhrase),
    /**
     * A sequence of a preposition and its complement is a prepositional phrase. The complement of a preposition is usually
     * a noun phrase (see examples 38 to 40), but may also be a clause or an adverb phrase. According to the categories
     * recommended here, a prepositional phrase may be analysed further into preposition and noun phrase. The examples
     * below demonstrate how this further analysis can be a recursive procedure.
     * <pre>
     * (38)     [PP en [NP sustitucion [PP de [NP los canales correspondientes [PP de [NP 50 baudios NP] PP] NP] PP] NP] PP].
     * (39)     [NP Fairbanks NP] [VP hummed [NP a few bars NP] VP] [PP in [NP a voice [VP made resonant 
     *      [PP by [NP the very weakness [PP of [NP his chest NP] PP] NP] PP] VP] NP] PP].
     * (40)     [PP En [NP el caso [PP de [NP un sistema mixto [PP en [NP el 
     *      [CL que [VP se utilicen [NP canales [PP con [NP tres velocidades 
     *      [PP de [NP modulacion NP] PP] diferentes NP] PP] NP] VP] CL] NP] PP] NP] PP] NP] PP]
     * </pre>
     * In a language such as Spanish, where a large proportion of the modification of nouns takes the form of a following
     * preposition de and another noun, this recursion is extremely prevalent, as in 40. In cases where the prepositional
     * phrase is complemented by a one word noun phrase, it may be advantageous to leave the analysis at this point, rather
     * than continuing to analyse further by enclosing the complement (see also one-word constituents).
     * <p>
     * (<a href="http://www.ilc.cnr.it/EAGLES96/segsasg1/node34.html#SECTION00052500000000000000">more information</a>)
     */
    PrepositionalPhrase(NounHeadedPhrase),
    /**
     * Conventional lexical unit consisting of a particular phrase (CC)
     * <p>
     * A phraseme, also called a set of thoughts, set phrase, idiomatic phrase, multi-word expression, or idiom, is a
     * multi-word or multi-morphemic utterance at least one of whose components is selectionally constrained or 
     * restricted by linguistic convention such that it is freely chosen.<br>
     * (definition from <a href="https://en.wikipedia.org/wiki/Phraseme">Wikipedia</a>)
     */
    Phraseme(Phrase),
    /**

     * This category is slightly more difficult to define, since there is disagreement over the extent of the verb phrase.
     * In particular, should the verb phrase include only the words that are verbs, or should it also include the
     * complements of the verb? In the examples given in this document, and in the sample texts in the appendices, we have
     * chosen to include the complements, but it must be noted that this is an open issue, and we are in no way implying
     * that this analysis is preferable to the alternative. The choice to be made at this level, i.e. the inclusion or
     * exclusion of verbal complements in the Verb Phrase, is shown by the examples in 27 and 28, 27 showing the inclusion
     * of the complement of the verb in the verb phrase and 28 excluding the complement:
     * <pre>
     * (27)     He [VP took up [NP a clothes brush NP] VP]
     * (28)     He [VP took up VP] [NP a clothes brush NP]
     * </pre>
     * An advantage in the type of analysis shown in 27 is that the relative levels of the constituents can be shown to a
     * greater extent -- i.e. complements of the verb are included in the verb phrase, while adjuncts and peripheral
     * adverbials are left at sentence level.
     * <p>
     * However, in a case where an adjunct occurs before the complement of the verb, the approaches used in 27 and 28 would
     * cause problems, since either both the adjunct and the complement would be included as daughters of the verb phrase,
     * or both would be daughters of the sentence, rather than keeping the complement as a daughter of the verb phrase and
     * the adjunct as a sister of the verb phrase. These problems may be solved by an additional notation, but at some
     * level, arbitrariness is inevitable.
     * <p>
     * Regardless of the choice made over the extent of the Verb Phrase, there arises a problem of discontinuous Verb
     * Phrases. A complex verbal construction may be discontinuous, e.g. the auxiliary and the main verb are separated in
     * inverted constructions in English, or the main verb is positioned at the end of the sentence in German and Dutch.
     * Such discontinuity can be avoided by having different labels and constituents for the auxiliary verb and the main
     * verb, resulting in an analysis as shown in the Dutch example 29 below:
     * <pre>
     * (29)     [NP Ze NP] [AUX zullen AUX] [ADVP er ADVP] [VP [NP de VN-agenda 
     *      [PP voor [NP het komende jaar NP] PP] NP] behandelen VP].
     * </pre>
     * and in the English interrogative inverted example 30, using the so-called `dummy auxiliary' do:
     * <pre>
     * (30)     [AUX Do AUX] [NP they NP] [VP confide [PP in you PP] VP]?
     * </pre>
     * As with Noun Phrases, Verb Phrases can be identified by a constituency test. In strong constituency languages like
     * English, the whole VP can be moved, but not part of it: compare 31 and 32:
     * <pre>
     * (31)       Give in to blackmail, I never will
     * (32)     *Give in, I never will to blackmail
     * </pre>
     * However, there are languages in which constituent tests do not work. These will typically be languages with flexible
     * word order, such as Finnish. 33 is an example of a discontinuous VP (Vilkuna 1989: 26):
     * <pre>
     * (33)     Maailmaa    nähnyt  hän on.
     *      world-Part  seen    he  is
     *      `He IS a widely-travelled person.'
     * </pre>
     * For Finnish, then, evidence for a VP is less convincing than it is for English, and a dependency approach seems the
     * more natural choice. (Covington (1990) provides a parsing strategy for variable word order languages and Covington
     * (1991) for parsing discontinuous constituents, both using a dependency syntax approach.)
     * <p>
     * In Italian also, constituency tests cannot be applied. This can be shown through the distribution of VP-adverbs
     * (e.g. completamente `completely', intenzionalmente `intentionally', attentamente `carefully') and S-adverbs (e.g.
     * probabilmente `probably', certamente `certainly'). In English, these different classes of adverbs have a different
     * distribution within the sentence. In contrast, in Italian, the distinct adverb classes cannot be distinguished on
     * the basis of their distribution in the sentence. S-adverbs and VP-adverbs can occur in the same positions within the
     * sentence, as illustrated in examples 34 to 37:
     * <pre>
     * (34)     Attentamente/certamente, il bambino ascoltó la storia
     *      `Carefully/certainly, the child listened to the story'
     * (35)     Il bambino attentamente/certamente ascoltó la storia
     *      `The child carefully/certainly listened to the story'
     * (36)     Il bambino ascoltó attentamente/certamente la storia
     *      `The child listened carefully/certainly to the story'
     * (37)     Il bambino ascoltó la storia attentamente/certamente
     *      `The child listened to the story carefully/certainly'
     * </pre>
     * Thus, in Italian as well as other languages, neither the position nor the syntactic context can help to decide
     * whether an adverb is an S-adverb or a VP-adverb; this can only be stated by considering its semantic content and the
     * way it relates to the content of the predicate or the sentence. This situation has consequences for the success of
     * standard VP-tests. 
     * <p>
     * (<a href="http://www.ilc.cnr.it/EAGLES96/segsasg1/node33.html">more information</a>)
     */
    VerbPhrase(LexicalCategory.Verb, Phrase),
    FiniteVerbPhrase(VerbPhrase),
    NonfiniteVerbPhrase(VerbPhrase),
    /**
     * VGNN Gerunds
     * <p>
     * A verb chunk having a gerund will be annotated as VGNN. For example,
     * <pre>
     * h18a. sharAba ((pInA_VM))_VGNN sehata ke liye hAnikAraka hE.
     *       'liquor' 'drinking' 'heath' 'for' 'harmful' 'is'
     *       “Drinking (liquor) is bad for health”
     * h19a. mujhe rAta meM ((khAnA_VM))_VGNN acchA lagatA hai
     *       'to me' 'night' 'in' 'eating' 'good' 'appeals'
     *       “I like eating at night”
     * h20a. ((sunane_VM meM_PSP))_VGNN saba kuccha acchA lagatA hE
     *       'listening' 'in' 'all' 'things' 'good' 'appeal' 'is'
     * </pre>
     * (Akshar Bharati, Dipti Misra Sharma, Lakshmi Bai, Rajeev Sangal (2006), AnnCorra : Annotating Corpora. Guidelines For POS And Chunk Annotation For Indian Languages, Tech. Rep., L anguage Technologies Research Centre IIIT, Hyderabad, version of 15-12-2006, http://ltrc.iiit.ac.in/tr031/posguidelines.pdf)
     */
    GerundVerbPhrase(NonfiniteVerbPhrase),
    /**
     * VGINF Infinitival Verb Chunk
     * <p>
     * This tag is to mark the infinitival verb form. In Hindi, both, gerunds and infinitive forms of the verb end with a -nA suffix. Since both behave functionally in a similar manner, the distinction is not very clear. However, languages such as Bangla etc have two different forms for the two types. Examples from Bangla are given below.
     * <pre>
     * b8. Borabela ((snAna karA))_VGNN SorIrera pokze BAlo
     *     'Morning' 'bath' 'do-verbal noun' 'health-gen' 'for' 'good'
     *     ‘Taking bath in the early morning is good for health”
     * b9. bindu Borabela ((snAna karawe))_VGINF BAlobAse
     *     'Bindu' 'morning' 'bath' 'take-inf' 'love-3pr'
     *     “Bindu likes to take bath in the early morning”
     * </pre>
     * In Bangla, the gerund form takes the suffix –A / -Ano, while the infinitive marker is –we. The syntactic distribution of these two forms of verbs is different. For example, the gerund form is allowed in the context of the word darakAra “necessary” while the infinitive form is not, as exemplified below:
     * <pre>
     * b10 Borabela ((snAna karA))_VGNN darakAra
     *     'Morning' 'bath' 'do-verbal noun' 'necessary'
     *     “It is necessary to take bath in the early morning”
     * b11. *Borabela ((snAna karawe))_VGINF darakAra
     * </pre>
     * Based on the above evidence from Bangla, the tag VGINF has been included to mark a verb chunk.
     * <p>
     * (Akshar Bharati, Dipti Misra Sharma, Lakshmi Bai, Rajeev Sangal (2006), AnnCorra : Annotating Corpora. Guidelines For POS And Chunk Annotation For Indian Languages, Tech. Rep., L anguage Technologies Research Centre IIIT, Hyderabad, version of 15-12-2006, http://ltrc.iiit.ac.in/tr031/posguidelines.pdf)     */
    InfinitiveVerbPhrase(NonfiniteVerbPhrase),
    /**
     * The maximal, syntactically independent, segments into which a text is subdivided, 
     * for parsing purposes, are normally considered to be sentences. In a written text, 
     * they are typically (though by no means invariably) delimited by an initial capital 
     * letter and a final full stop (`.') or other terminal punctuation. It is convenient 
     * to accept this primary orthographic definition of `sentence' for the purposes of 
     * syntactic annotation. However, a sentence, so defined, may be either a full 
     * sentence (9) or a `grammatically incomplete' one (10).
     * <pre>
     * (9)      [S This is a sentence. S]
     * (10)     [S Well done. S]
     * </pre>
     * The same applies to sentences included within other sentences, as in (11)
     * <pre>
     * (11)     [S [S ``Well done'', S] she said. S] }
     * </pre>
     * ``Well done'' in 11 is labelled as a sentence, since it clearly has an independent 
     * syntactic status equivalent to those of 9, even though it is included in another 
     * sentence. This inclusion of one independent sentence within another is found both 
     * with reported speech and elsewhere. Phenomena such as those illustrated in 10 are 
     * by no means exceptional in text corpora.
     * <p>
     * In transcriptions of spoken discourse, there is no simple answer to the question 
     * ``What is a sentence?''. Some transcriptions, based on standard orthography, yield 
     * de facto sentences in the form of units beginning with a capital letter and closing 
     * with a terminal punctuation mark. For these, there is no problem in recognising the 
     * primary sentential segments and delimiting them by [S ... S], even though these 
     * segments frequently lack the canonical structure of a complete written sentence. 
     * Moreover, even in other transcriptions, where the standard orthographic practices of 
     * sentence delimitation are avoided, it is possible to identify `primary segments' 
     * analogous to the written sentence, viz. the primary units into which the transcribed 
     * discourse is divided for parsing purposes. For spoken as well as written language, 
     * then, the [S] unit may be retained, although it may be interpreted differently, and 
     * some other term, such as `primary segment', may be preferred to `sentence'.
     * <p>
     * We conclude by recommending, for the syntactic annotation of any text (including a 
     * transcription of spoken language), an exhaustive division of the text into units 
     * labelled [S ... S].
     * (http://www.ilc.cnr.it/EAGLES96/segsasg1/node30.html#SECTION00052100000000000000)     */
    Sentence,
    /**
     * Clause is the class of constructions that form minimal sentential units. 
     * They must include a predicate, all arguments of the predicate, and all 
     * modifiers of the predicate and the arguments.
     * (<a href="http://purl.org/linguistics/gold/Clause">more Information</a>)
     * <p>
     */

    //Other phrase types not defined in OLIA
    /**
     * Quantifier phrase (e.g. multi token numbers)<br>
     * <b>NOTE:</b> Not present in Olia
     */
    QuantifierPhrase(LexicalCategory.Quantifier)
    ;
    static final String OLIA_NAMESPACE = "http://purl.org/olia/olia.owl#";

    private final LexicalCategory category;
    private final Collection<PhraseCategory> parents;
    private final String uri;

    PhraseCategory() {
        this(null, (LexicalCategory)null);
    }

    PhraseCategory(LexicalCategory lexCat) {
        this(null, lexCat);
    }

    PhraseCategory(PhraseCategory... parent) {
        this(null, null, parent);
    }

    PhraseCategory(String name, PhraseCategory... parent) {
        this(name, null, parent);
    }

    PhraseCategory(LexicalCategory category, PhraseCategory... parent) {
        this(null,category,parent);
    }
    PhraseCategory(String name, LexicalCategory category, PhraseCategory... parent) {
        this.uri = OLIA_NAMESPACE + (name == null ? name() : name);
        this.parents = parent == null || parent.length < 1 ? Collections.emptySet() : Arrays.asList(parent);
        Set<PhraseCategory> toProcess = new HashSet<PhraseCategory>(parents);
        while (!toProcess.isEmpty()) {
            Iterator<PhraseCategory> it = toProcess.iterator();
            PhraseCategory p = it.next();
            it.remove();
            if(category == null){
                category = p.category;
            } else if(p.category != null && !p.category.equals(category)){
                throw new IllegalStateException("Multiple LexicalCategories found for '"+name+"'");
            }
        }
        this.category = category;
    }

    public LexicalCategory category() {
        return category;
    }

    public boolean isParent() {
        return parents.isEmpty();
    }

    public Collection<PhraseCategory> parents() {
        return parents;
    }

    public String getUri() {
        return uri;
    }

    public Set<PhraseCategory> hierarchy() {
        return transitiveClosureMap.get(this);
    }

    @Override
    public String toString() {
        return String.format("olia:%s", 
            uri.substring(OLIA_NAMESPACE.length()));
    }
    
    /**
     * Mapping of {@link LexicalCategory} to {@link PhraseCategory}. Mainly for
     * backward compatibility to the old {@link PhraseTag} using {@link LexicalCategory}
     */
    private static final Map<LexicalCategory, PhraseCategory> lexCat2PhraseCat;
    
    /**
     * This is needed because one can not create EnumSet instances before the
     * initialization of an Enum has finished.<p>
     * To keep using the much faster {@link EnumSet} a static member initialised
     * in an static {} block is used as a workaround. The {@link Tense#getTenses()}
     * method does use this static member instead of a member variable
     */
    private static final Map<PhraseCategory,Set<PhraseCategory>> transitiveClosureMap;
    
    static {
        transitiveClosureMap = new EnumMap<>(PhraseCategory.class);
        lexCat2PhraseCat = new EnumMap<>(LexicalCategory.class);
        for(PhraseCategory pc : PhraseCategory.values()){
            Set<PhraseCategory> parents = EnumSet.of(pc);
            for(PhraseCategory pcParent : pc.parents()){
                Set<PhraseCategory> transParents = transitiveClosureMap.get(pcParent);
                if(transParents != null){
                    parents.addAll(transParents);
                } else if(pcParent != null){
                    parents.add(pcParent);
                } // else no parent
            }
            transitiveClosureMap.put(pc, parents);
            if(pc.category() != null && !lexCat2PhraseCat.containsKey(pc.category)){
                lexCat2PhraseCat.put(pc.category(), pc);
            }
        }
    }
    /**
     * Getter for the default {@link PhraseCategory} for a given {@link LexicalCategory}.
     * @param lc the lexical category
     * @return the phrase category
     */
    public static PhraseCategory getPhraseCategory(LexicalCategory lc) {
        return lexCat2PhraseCat.get(lc);
    }
    
}
