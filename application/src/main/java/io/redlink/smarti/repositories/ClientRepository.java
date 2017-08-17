package io.redlink.smarti.repositories;

import io.redlink.smarti.model.Conversation;
import org.springframework.data.repository.CrudRepository;

/**
 * @author Thomas Kurz (thomas.kurz@redlink.co)
 * @since 17.08.17.
 */
public interface ClientRepository extends CrudRepository<Conversation, String> {
}
