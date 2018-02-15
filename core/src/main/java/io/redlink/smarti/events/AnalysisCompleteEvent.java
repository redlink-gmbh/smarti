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
package io.redlink.smarti.events;

import java.util.Date;

import io.redlink.smarti.model.Analysis;
import io.redlink.smarti.model.Client;
import io.redlink.smarti.model.Conversation;
import io.redlink.smarti.model.config.Configuration;

/**
 * An event that the processing of a Conversation is complete
 */
public interface AnalysisCompleteEvent {
    /**
     * The {@link Client} the analysis was done for
     * @return
     */
    Client getClient();
    /**
     * The {@link Conversation} the analysis was done for
     * @return
     */
    Conversation getConversation();
    /**
     * The date the Analysis was done for. This is the latest date from
     * {@link Conversation#getLastModified()} and {@link Configuration#getModified()}. It
     * allows to check if this analysis is still up-to-date with changes in the
     * conversation or the configuration
     * @return
     */
    Date getDate();
    /**
     * The Analysis
     * @return
     */
    Analysis getAnalysis();
}
