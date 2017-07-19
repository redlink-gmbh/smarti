package io.redlink.smarti.services;

import java.util.Date;

import org.apache.commons.lang3.StringUtils;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Component;

import io.redlink.smarti.exception.BadArgumentException;
import io.redlink.smarti.exception.ConflictException;
import io.redlink.smarti.exception.NotFoundException;
import io.redlink.smarti.model.config.Configuration;
import io.redlink.smarti.model.config.SmartiConfiguration;
import io.redlink.smarti.repo.ConfigurationRepo;

@Component
public class ConfigurationService {
    
    private final ConfigurationRepo configRepo;
    
    public ConfigurationService(ConfigurationRepo configRepo){
        this.configRepo = configRepo;
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
    public Configuration storeConfiguration(String client, SmartiConfiguration configuration){
        if(configuration == null){
            throw new NullPointerException("The parsed Smarti configuration MUST NOT be NULL");
        }
        if(StringUtils.isBlank(client)){
            throw new BadArgumentException("client", client, "The client of a configuration MUST NOT be NULL nor blanl!");
        }
        Configuration c = configRepo.findByClient(client);
        if(c == null){
            c = new Configuration();
            c.setClient(client);
            c.setSmarti(configuration);
            return create(c);
        } else {
            c.setSmarti(configuration);
            return save(c);
        }
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

}
