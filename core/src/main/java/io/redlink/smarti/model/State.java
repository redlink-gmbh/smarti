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

package io.redlink.smarti.model;

/**
 * The state of the Token. Intended to be used for
 * acquiring user feedback (e.g. for improving the training sets for
 * machine learning models)
 */
public enum State {
    /**
     * Initial state for extracted tokens
     */
    Suggested,
    /**
     * State for tokens that where confirmed (e.g. used in a query)
     */
    Confirmed,
    /**
     * State for tokens that where rejected (e.g. removed from a
     * proposed query)
     */
    Rejected
}
