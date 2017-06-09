/*
 * Copyright (c) 2016 Redlink GmbH
 */
package io.redlink.smarti.processor.hasso;

import io.redlink.smarti.api.TemplateBuilder;
import io.redlink.smarti.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by jakob on 02.09.16.
 */
@Component
public class ApplicationHelpTemplateBuilder extends TemplateBuilder {

    @Autowired
    private ApplicationHelpTemplate applicationHelpTemplate = new ApplicationHelpTemplate();

    @Override
    protected TemplateDefinition getDefinition() {
        return applicationHelpTemplate;
    }

    @Override
    protected Set<Integer> updateTemplate(Template template, Conversation conversation, int startMsgIdx) {
        if(template.getState() == State.Confirmed || template.getState() == State.Rejected){
            return null; //do not update this template
        }

        final HashSet<Integer> indexes = new HashSet<>();

        Slot from = applicationHelpTemplate.getSlot(ApplicationHelpTemplate.SUPPORT_TYPE, template);
        if (from == null) {
            from = applicationHelpTemplate.createSlot(ApplicationHelpTemplate.SUPPORT_TYPE);
            template.getSlots().add(from);
        }
        if (from.getTokenIndex() < 0) {
            for (int i = 0; i < conversation.getTokens().size(); i++) {
                final Token t = conversation.getTokens().get(i);
                if (t.getState() == State.Rejected) continue;
                if (!(t.getMessageIdx() < 0 || t.getMessageIdx() >= startMsgIdx)) continue;

                if (t.getType() == Token.Type.QuestionIdentifier) {
                    from.setTokenIndex(i);
                    indexes.add(i);
                }
            }
        }

        final List<Slot> keywords = applicationHelpTemplate.getSlots(ApplicationHelpTemplate.KEYWORD, template);
        final Set<Integer> usedTokens = keywords.stream().map(Slot::getTokenIndex).filter(i -> i >= 0).collect(Collectors.toSet());
        for (int i = 0; i < conversation.getTokens().size(); i++) {
            if (usedTokens.contains(i)) continue; // do not use tokens twice
            final Token t = conversation.getTokens().get(i);
            if (t.getState() == State.Rejected) continue;
            if (!(t.getMessageIdx() < 0 || t.getMessageIdx() >= startMsgIdx)) continue;

            if (t.getType() == Token.Type.Keyword && (t.hasHint(SapKeywordsVocab.HINT) || t.hasHint(DbKonzernSynonymVocab.HINT))) {
                // That's a token we are interested in!
                final Optional<Slot> emptySlot = keywords.stream()
                        .filter(s -> s.getTokenIndex() < 0)
                        .findAny();
                if (emptySlot.isPresent()) {
                    emptySlot.get().setTokenIndex(i);
                } else {
                    final Slot kwd = applicationHelpTemplate.createSlot(ApplicationHelpTemplate.KEYWORD);
                    kwd.setTokenIndex(i);
                    template.getSlots().add(kwd);
                }
                usedTokens.add(i);
                indexes.add(i);
            }

        }

        return indexes;
    }

    @Override
    protected void initializeTemplate(Template queryTemplate) {
        queryTemplate.getSlots().addAll(Arrays.asList(
                applicationHelpTemplate.createSlot(ApplicationHelpTemplate.SUPPORT_TYPE),
                applicationHelpTemplate.createSlot(ApplicationHelpTemplate.KEYWORD)
        ));

    }
}
