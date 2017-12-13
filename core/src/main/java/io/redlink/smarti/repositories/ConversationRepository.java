/*
 * Copyright 2017 Redlink GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package io.redlink.smarti.repositories;

import io.redlink.smarti.model.Conversation;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Conversation Repository
 *
 * @author Sergio Fernández
 */
public interface ConversationRepository extends PagingAndSortingRepository<Conversation, ObjectId>, ConversationRepositoryCustom {

    Conversation findByOwnerAndId(ObjectId owner, ObjectId id);
    
    Page<Conversation> findByOwner(ObjectId owner, Pageable paging);

    List<Conversation> findByOwner(ObjectId owner);

    Page<Conversation> findByOwnerIn(Set<ObjectId> clientIDs, Pageable paging);
}
