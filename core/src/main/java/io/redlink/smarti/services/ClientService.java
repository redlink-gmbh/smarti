package io.redlink.smarti.services;

import com.google.common.collect.ImmutableMap;
import com.mongodb.DuplicateKeyException;
import io.redlink.smarti.exception.ConflictException;
import io.redlink.smarti.model.Client;
import io.redlink.smarti.model.config.Configuration;
import io.redlink.smarti.repositories.ClientRepository;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Collection;
import java.util.Date;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Thomas Kurz (thomas.kurz@redlink.co)
 * @since 17.08.17.
 */
@Service
public class ClientService {

    @Autowired
    ClientRepository clientRepository;

    @Autowired
    ConfigurationService configurationService;

    private static final String NAME_PATTERN = "[a-z0-9-_\\.:]+";

    public Iterable<Client> list() {
        return clientRepository.findAll();
    }

    public Client get(ObjectId id) {
        return clientRepository.findOne(id);
    }
    
    public Client getDefaultClient(){
        return clientRepository.findOneByDefaultClientTrue();
    }

    public Client save(Client client) {

        if(client.getId() != null) {
            Client client_old = clientRepository.findOne(client.getId());

            if(client_old == null) {
                throw new ConflictException(Client.class, "id", "New clients may not have an id set");
            }
            
            if(!client_old.getName().equals(client.getName())) {
                validateClientName(client);
            }
        } else {
            if(!isProperClientName(client.getName())) {
                throw new IllegalArgumentException("Client name must match pattern: " + NAME_PATTERN);
            }
        }

        if(client.isDefaultClient()) {
            clientRepository.save(clientRepository.findByDefaultClientTrue().stream().map(
                    c -> {
                        c.setDefaultClient(false);
                        return c;
                    }
            ).collect(Collectors.toList()));
        }
        
        //save the client
        client.setLastUpdate(new Date());
        try {
            client = clientRepository.save(client);
        } catch (DuplicateKeyException | org.springframework.dao.DuplicateKeyException e) {
            throw new ConflictException(Client.class, "name", "A Client with the name '" + client.getName() + "' already exists!");
        }
        //init the client configuration
        initClientConfiguration(client);
        return client;
    }

    public void delete(Client client) {
        if(client != null && client.getId() != null){
            Client stored = clientRepository.findOne(client.getId());
            if(stored != null){ //only delete configuration if the client is known
                configurationService.deleteConfiguration(stored);
            }
            clientRepository.delete(client);
        } //null or invalid client parsed
    }

    public boolean exists(ObjectId id) {
        return clientRepository.exists(id);
    }

    public boolean existsByName(String clientName) {
        return clientRepository.existsByName(clientName);
    }
    
    public Client getByName(String clientName){
        return clientRepository.findOneByName(clientName);
    }

    private static boolean isProperClientName(String name) {
        return name.matches(NAME_PATTERN);
    }
    
    private Client validateClientName(Client client) {
        if(client.getName() == null || client.getName().trim().isEmpty()){
            throw new ConflictException(Client.class, "name", "The client name MUST NOT be NULL nor empty!");
        }

        if(!isProperClientName(client.getName())) {
            throw new ConflictException(Client.class, "name", "Client name must match pattern: " + NAME_PATTERN);
        }
        if(clientRepository.existsByName(client.getName())) {
            throw new ConflictException(Client.class, "name", "The Client name '" + client.getName() + "' is not unique");
        }
        return client;
    }
    
    private Client initClientConfiguration(Client client) {
        if(!configurationService.isConfiguration(client)) {
            Client defaultClient = client.isDefaultClient() ? null : //if this is the new default client start with an empty configuration
                getDefaultClient(); //else try to retrieve the default client
            Configuration defaultConf = defaultClient == null ? null : //no default client
                configurationService.getClientConfiguration(defaultClient); //end the default configuration
            if(defaultConf != null) {
                configurationService.createConfiguration(client, defaultConf);
            } else {
                configurationService.createConfiguration(client);
            }
        }
        return client;
    }

    public Iterable<Client> list(Iterable<ObjectId> clients) {
        return clientRepository.findAll(clients);
    }
    
}
