package io.redlink.smarti.repo;

import io.redlink.smarti.model.Client;
import io.redlink.smarti.model.Conversation;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

/**
 * @author Thomas Kurz (thomas.kurz@redlink.co)
 * @since 17.08.17.
 */
public interface ClientRepository extends CrudRepository<Client, ObjectId> {
    public boolean existsByName(String name);
    public Client findOneByDefaultClientTrue();
    public List<Client> findByDefaultClientTrue();
    public Client findOneByName(String name);
}
