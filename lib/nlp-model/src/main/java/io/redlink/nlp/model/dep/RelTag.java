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
package io.redlink.nlp.model.dep;

import io.redlink.nlp.model.Token;
import io.redlink.nlp.model.tag.Tag;


/**
 * Represents the type of a {@link Relation} between two {@link Token}s
 * 
 * @author Cristian Petroaca
 * @author Rupert Westenthaler
 * 
 */
public class RelTag extends Tag<RelTag> {

	/**
	 * The actual grammatical relation object
	 */
	private GrammaticalRelation rel;

	public RelTag(String tag) {
		super(tag);
	}

	public RelTag(String tag, GrammaticalRelation rel) {
		this(tag);
		this.rel = rel;
	}

	/**
	 * Getter for the {@link GrammaticalRelation} type
	 * @return The {@link GrammaticalRelation} type or <code>null</code> if not known
	 */
	public GrammaticalRelation getRelation() {
		return rel;
	}

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((rel == null) ? 0 : rel.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        if (getClass() != obj.getClass())
            return false;
        RelTag other = (RelTag) obj;
        if (rel != other.rel)
            return false;
        return true;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("rel('").append(tag).append('\'');
        if(rel != null){
            sb.append(',').append(rel.name());
        }
        return sb.append(')').toString();
    }
	
}
