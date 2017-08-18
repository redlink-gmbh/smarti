package io.redlink.smarti.repositories;

import io.redlink.smarti.model.Client;
import io.redlink.smarti.model.Conversation;
import org.bson.types.ObjectId;
import org.springframework.data.repository.CrudRepository;

/**
 * @author Thomas Kurz (thomas.kurz@redlink.co)
 * @since 17.08.17.
 */
public interface ClientRepository extends CrudRepository<Client, ObjectId> {
    public boolean existsByName(String name);
}
