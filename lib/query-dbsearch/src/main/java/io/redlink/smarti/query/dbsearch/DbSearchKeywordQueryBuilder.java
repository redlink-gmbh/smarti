package io.redlink.smarti.query.dbsearch;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import io.redlink.smarti.model.Conversation;
import io.redlink.smarti.model.Slot;
import io.redlink.smarti.services.TemplateRegistry;

/**
 * Variant of the {@link DbSearchQueryBuilder} that builds a
 * {@link DbSearchQuery} that only includes {@link Slot}s with
 * the Role {@link DbSearchTemplateDefinition#ROLE_KEYWORD}
 * @author Rupert Westenthaler
 *
 */
@Component
@ConditionalOnProperty("dbsearch.solr")
public class DbSearchKeywordQueryBuilder extends DbSearchQueryBuilder {

    public DbSearchKeywordQueryBuilder(TemplateRegistry registry) {
        super(registry, "keyword");
    }

    @Override
    protected boolean acceptSlot(Slot slot, Conversation conversation) {
        return DbSearchTemplateDefinition.ROLE_KEYWORD.equals(slot.getRole()); 
    }
    
    @Override
    protected String getQueryTitle(){
        return "DB Search Keyword Search";
    }

    
}
