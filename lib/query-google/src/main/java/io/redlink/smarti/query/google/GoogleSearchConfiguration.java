/*
 * Copyright 2019 DB Systel GmbH
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
 */
package io.redlink.smarti.query.google;

import java.util.*;

import org.springframework.boot.context.properties.ConfigurationProperties;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import io.redlink.smarti.model.config.ComponentConfiguration;

@ConfigurationProperties(prefix="query.google")
@JsonInclude(content=Include.ALWAYS) //the UI needs to know all possible properties
public class GoogleSearchConfiguration extends ComponentConfiguration implements Cloneable {

    private String googleEndpoint = "https://www.googleapis.com/customsearch/v1?key=YOUR_KEY&cx=YOUR_ID";
    private Map<String,Object> defaults = new HashMap<>();
    private ResultConfig result = new ResultConfig();    


    public String getGoogleEndpoint() {
        return googleEndpoint;
    }
    public void setGoogleEndpoint(String googleEndpoint) {
        this.googleEndpoint = googleEndpoint;
    }

    public ResultConfig getResult() {
        return result;
    }
    public void setResult(ResultConfig result) {
        this.result = result;
    }

    public Map<String, Object> getDefaults() {
        return defaults;
    }

    /**
      * Allows to configure the mappings from fields in the index
      * to fields shown by the templates presenting results
      * @author Rupert Westenthaler
      *
      */
    @JsonInclude(content=Include.ALWAYS) //the UI needs to know all possible properties
    public static class ResultConfig {

        private Mappings mappings = new Mappings();
        private int numOfRows = 10;

        public Mappings getMappings() {
            return mappings;
        }

        public void setMappings(Mappings mappings) {
            this.mappings = mappings;
        }

        public int getNumOfRows() {
            return numOfRows;
        }

        public void setNumOfRows(int numOfRows) {
            this.numOfRows = numOfRows;
        }

        public static class Mappings {

            private String source, title, description, link;

            public String getSource() {
                return source;
            }

            public void setSource(String source) {
                this.source = source;
            }

            public String getTitle() {
                return title;
            }

            public void setTitle(String title) {
                this.title = title;
            }

            public String getDescription() {
                return description;
            }

            public void setDescription(String description) {
                this.description = description;
            }

            public String getLink() {
                return link;
            }

            public void setLink(String link) {
                this.link = link;
            }
        }
    }

    @Override
    public GoogleSearchConfiguration clone() {
        GoogleSearchConfiguration clone = new GoogleSearchConfiguration();
        copyState(clone);
        clone.googleEndpoint = googleEndpoint;
        return clone;
    }
}
