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
package io.redlink.smarti.configuration;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.fasterxml.jackson.databind.type.MapLikeType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import io.redlink.smarti.model.config.ComponentConfiguration;
import io.redlink.smarti.model.config.Configuration;
import io.redlink.smarti.webservice.pojo.AuthContext;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Scope;
import org.springframework.core.MethodParameter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;

@org.springframework.context.annotation.Configuration
public class RestServiceConfiguration extends WebMvcConfigurerAdapter {

    private final Logger log = LoggerFactory.getLogger(RestServiceConfiguration.class);

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> argumentResolvers) {
        argumentResolvers.add(new AuthContextResolver());
    }

    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        log.debug("Configuring MessageConverters");
        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter(objectMapper());
        converters.add(converter);
    }

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public ObjectMapper objectMapper() {
        final Jackson2ObjectMapperBuilder builder = new Jackson2ObjectMapperBuilder();
        // serialize ObjectId,URI as String
        builder.serializerByType(ObjectId.class, new ToStringSerializer());
        builder.serializerByType(URI.class, new ToStringSerializer());

        builder.indentOutput(true);

        final ObjectMapper objectMapper = builder.build();

        log.debug("Register module 'smartiConfigModule'");
        objectMapper.registerModule(createSmartiConfigModule(objectMapper));

        return objectMapper;
    }

    private SimpleModule createSmartiConfigModule(ObjectMapper objectMapper) {
        final SimpleModule smartiConfigModule = new SimpleModule();

        smartiConfigModule.addSerializer(Configuration.class,
                new JsonSerializer<Configuration>() {
                    @Override
                    public void serialize(Configuration value, JsonGenerator jgen, SerializerProvider provider) throws IOException {
                        log.debug("Serializing Configuration with custom serializer");
                        final TypeFactory tf = provider.getTypeFactory();
                        final MapLikeType smartiConfigType = tf.constructMapLikeType(Map.class,
                                tf.constructType(String.class),
                                tf.constructCollectionLikeType(List.class,
                                        ComponentConfiguration.class));
                        objectMapper.writerFor(smartiConfigType).writeValue(jgen, value.getConfig());
                    }
                });
        smartiConfigModule.addDeserializer(Configuration.class,
                new JsonDeserializer<Configuration>() {
                    @Override
                    public Configuration deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
                        log.debug("Deserializing Configuration with custom deserializer");

                        final TypeFactory tf = ctxt.getTypeFactory();
                        final MapLikeType smartiConfigType = tf.constructMapLikeType(Map.class,
                                tf.constructType(String.class),
                                tf.constructCollectionLikeType(List.class,
                                        ComponentConfiguration.class));

                        final Configuration configuration = new Configuration();
                        configuration.setConfig(objectMapper.readerFor(smartiConfigType).readValue(p, smartiConfigType));
                        return configuration;
                    }

                });

        return smartiConfigModule;
    }

    private class AuthContextResolver implements HandlerMethodArgumentResolver {
        @Override
        public boolean supportsParameter(MethodParameter parameter) {
            return AuthContext.class.equals(parameter.getParameterType());
        }

        @Override
        public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer, NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
            final String token = webRequest.getHeader("X-Auth-Token");
            final Authentication authentication = SecurityContextHolder.getContext()
                    .getAuthentication();
            return new AuthContext(token, authentication);
        }
    }
}
