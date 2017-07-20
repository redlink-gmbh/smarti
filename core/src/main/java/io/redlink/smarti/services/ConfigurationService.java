package io.redlink.smarti.services;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import io.redlink.smarti.api.config.Configurable;
import io.redlink.smarti.exception.BadArgumentException;
import io.redlink.smarti.exception.ConflictException;
import io.redlink.smarti.exception.InvalidOrMissingFieldsException;
import io.redlink.smarti.exception.NotFoundException;
import io.redlink.smarti.model.config.ComponentConfiguration;
import io.redlink.smarti.model.config.Configuration;
import io.redlink.smarti.repo.ConfigurationRepo;

@Component
public class ConfigurationService {
    
    private final Logger log = LoggerFactory.getLogger(getClass());
    
    private final ConfigurationRepo configRepo;
    private final Map<String,Map<String,Configurable<?>>> configurableComponents;
    
    public ConfigurationService(ConfigurationRepo configRepo, Optional<Collection<Configurable<?>>> configureableComponents){
        this.configRepo = configRepo;
        //build up the lookup map with all configureable components
        Map<String,Map<String,Configurable<?>>> ccs = new HashMap<>();
        if(configureableComponents.isPresent()){
            configureableComponents.get().forEach(cc -> {
                Map<String,Configurable<?>> catCcs = ccs.get(cc.getComponentCategory());
                if(catCcs == null){
                    catCcs = new HashMap<>();
                    ccs.put(cc.getComponentCategory(), catCcs);
                }
                if(catCcs.containsKey(cc.getComponentName())){
                    throw new IllegalStateException("Two configureable components of the category '" + cc.getComponentCategory()
                        + "' do use the same component name '" + cc.getComponentName() + "' (first: " 
                        + catCcs.get(cc.getComponentName()).getClass() + " | second: " + cc.getClass()+ ")!");
                }
                catCcs.put(cc.getComponentName(), cc);
            });
        }
        this.configurableComponents = Collections.unmodifiableMap(ccs);
    }

    
    public Configuration getConfiguration(ObjectId id){
        return configRepo.findOne(id);
    }
    
    public Configuration getConfiguration(String client){
        return configRepo.findByClient(client);
    }
    
    public boolean isConfiguration(String client){
        return configRepo.existsByClient(client);
    }

    /**
     * Deletes any configuration for the parsed client
     * @param client the client to delete the configuration for
     */
    public void deleteConfiguration(String client){
        Configuration c = getConfiguration(client);
        if(c != null && c.getId() != null){
            delete(c.getId());
        }
    }
    
    /**
     * Stores the {@link SmartiConfiguration} for the client. If necessary this will
     * create a new {@link Configuration}.
     * @param client the client
     * @param configuration the {@link SmartiConfiguration}
     * @return the {@link Configuration} object (created or updated) holding the parsed
     * {@link SmartiConfiguration}
     */
    public Configuration storeConfiguration(String client, Map<String,List<ComponentConfiguration>> config){
        if(config == null){
            throw new NullPointerException("The parsed configuration MUST NOT be NULL");
        }
        if(StringUtils.isBlank(client)){
            throw new BadArgumentException("client", client, "The client of a configuration MUST NOT be NULL nor blanl!");
        }
        validate(config);
        Configuration c = configRepo.findByClient(client);
        if(c == null){
            c = new Configuration();
            c.setClient(client);
            c.setConfig(config);
            return create(c);
        } else {
            c.setConfig(config);
            return save(c);
        }
    }
    
    /**
     * Validates a SmartiConfiguration against the active {@link Configurable} components
     * @param configuration the configuration to validate
     */
    private void validate(Map<String,List<ComponentConfiguration>> configuration) {
        Set<String> missing = new HashSet<>();
        Map<String,String> conflicting = new HashMap<>();
        //validate queryBuilder configurations
        configuration.entrySet().forEach(confCategory -> {
            //get all configurable components for the current category
            Map<String,Configurable<?>> catConfComp = configurableComponents.get(confCategory.getKey());
            AtomicInteger idx = new AtomicInteger();
            confCategory.getValue().forEach( conf -> {
                int i = idx.getAndIncrement();
                Configurable cc = catConfComp.get(conf.getType());
                if(cc == null){ //this configuration in unknown
                    conf.setUnbound(true);
                } else {
                    String pathPrefix = cc.getComponentCategory() + '[' + i + "].";
                    conf.setUnbound(false);
                    if(cc.getComponentType().isAssignableFrom(conf.getClass())){
                        Set<String> m = new HashSet<>();
                        Map<String,String> c = new HashMap<>();
                        if(!cc.validate(conf, m, c)){
                            //collect the missing and conflicting and add them with the pathPrefix of the current
                            //configuration to the main lists
                            m.stream().filter(StringUtils::isNoneBlank)
                                .map(f -> StringUtils.join(pathPrefix,f))
                                .collect(Collectors.toCollection(() -> missing)); //add to existing missing
                            c.entrySet().stream().filter(e -> StringUtils.isNoneBlank(e.getKey()))
                                .collect(Collectors.toMap(
                                        e -> StringUtils.join(pathPrefix,e.getKey()), 
                                        e -> e.getValue(),
                                        (u,v) -> { throw new IllegalStateException(String.format("Duplicate key %s", u)); }, 
                                        () -> conflicting)); //ad to the existing conflicting map
                        } //a valid config
                    } else { //config has unexpected type
                        conflicting.put(pathPrefix, "Configuration has unexpected type '" + 
                                conf.getClass().getName() + "' (expected: '" + cc.getComponentType().getName() + "')");
                    }
                    
                }
            });
        });
        if(!missing.isEmpty() || !conflicting.isEmpty()){
            throw new InvalidOrMissingFieldsException(Configuration.class, conflicting, missing);
        } //else validation succeeded
        
    }


    //NOTE: for now create(..) and save(..) are private as store(client, smartiConfig) shoudl be sufficient
    private Configuration create(Configuration c){
        if(c == null){
            throw new NullPointerException();
        }
        if(c.getId() != null){
            throw new BadArgumentException("id", c.getId(), "The server assigned id of a configuration MUST be NULL on creation!");
        }
        if(StringUtils.isBlank(c.getClient())){
            throw new BadArgumentException("client", c.getClient(), "The client of a configuration MUST NOT be NULL nor blanl!");
        }
        if(configRepo.existsByClient(c.getClient())){
            throw new BadArgumentException("client", c.getClient(), "A configuration for this client is already present!");
        }
        c.setCreated(new Date());
        c.setModified(c.getCreated());
        return configRepo.save(c);
    }

    private Configuration save(Configuration c){
        if(c == null){
            throw new NullPointerException();
        }
        if(c.getId() == null){
            throw new BadArgumentException("id", c.getId(), "The id of a configuration MUST NOT be NULL when saved!");
        }
        Configuration stored = configRepo.findOne(c.getId());
        if(stored == null){
            throw new NotFoundException(Configuration.class, c.getId());
        }
        if(!stored.getClient().equals(c.getClient())){
            throw new ConflictException(Configuration.class, "client", "The client can not be modified for an existing configuration!");
        }
        c.setCreated(stored.getCreated()); //do not allow to change creation dates
        c.setModified(new Date());
        return configRepo.save(c);
    }
    
    private void delete(ObjectId id){
        configRepo.delete(id);
    }


    public Map<String, List<ComponentConfiguration>> getDefaultConfiguration() {
        Map<String, List<ComponentConfiguration>> config = new HashMap<>();
        configurableComponents.values().stream().flatMap(m -> m.values().stream())
            .forEach(cc -> {
                ComponentConfiguration conf = cc.getDefaultConfiguration();
                if(conf == null) {
                    try {
                        conf = cc.getComponentType().newInstance();
                    } catch (InstantiationException |IllegalAccessException e) {
                        log.warn("Unable to create instance of {} using default constructor", cc.getComponentType());
                    }
                }
                if(conf != null){
                    conf.setType(cc.getComponentName());
                    conf.setUnbound(false);
                    conf.setEnabled(true);
                    List<ComponentConfiguration> ccs = config.get(cc.getComponentCategory());
                    if(ccs == null){
                        ccs = new LinkedList<>();
                        config.put(cc.getComponentCategory(), ccs);
                    }
                    ccs.add(conf);
                }
            });
        return config;
    }
    /**
     * Creates a configuration for a specific category and type
     * @param category the category
     * @param type the type
     * @return the default configuration or <code>null</code> if no configureable component was
     * found for the parsed parameter
     */
    public ComponentConfiguration getDefaultConfiguration(String category, String type){
        Map<String,Configurable<?>> ccc = configurableComponents.get(category);
        if(ccc != null){
            Configurable<?> cc = ccc.get(type);
            if(cc != null){
                ComponentConfiguration c = cc.getDefaultConfiguration();
                if(c == null){
                    try {
                        c = cc.getComponentType().newInstance();
                    } catch (InstantiationException |IllegalAccessException e) {
                        log.warn("Unable to create instance of {} using default constructor", cc.getComponentType());
                        return null;
                    }
                }
                c.setType(cc.getComponentName());
                c.setUnbound(false);
                c.setEnabled(true);
                return c;
            }
        }
        return null;
    }

}
