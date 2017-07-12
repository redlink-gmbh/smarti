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

package io.redlink.smarti.exception;

/**
 * Interface that allows to provide structured information
 * about the cause of an exception.
 * 
 * @author Rupert Westenthaler
 *
 * @param <T>
 */
public interface DataException<T> {
    
    /**
     * Getter for structured information describing the cause for the exception
     * @return the structured information or <code>null</code> if not available
     */
    T getData();
    
}
