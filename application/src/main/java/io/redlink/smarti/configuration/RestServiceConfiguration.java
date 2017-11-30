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
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.fasterxml.jackson.databind.type.MapLikeType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import io.redlink.smarti.model.config.ComponentConfiguration;
import io.redlink.smarti.webservice.pojo.AuthContext;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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

@Configuration
public class RestServiceConfiguration extends WebMvcConfigurerAdapter {

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> argumentResolvers) {
        argumentResolvers.add(new AuthContextResolver());
    }

    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter(objectMapper());
        converters.add(converter);
    }

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public ObjectMapper objectMapper() {
        Jackson2ObjectMapperBuilder builder = new Jackson2ObjectMapperBuilder();
        // serialize ObjectId,URI as String
        builder.serializerByType(ObjectId.class, new ToStringSerializer());
        builder.serializerByType(URI.class, new ToStringSerializer());

        builder.indentOutput(true);

        final ObjectMapper objectMapper = builder.build();

        registerSmartiConfigModule(objectMapper);

        return objectMapper;
    }

    private ObjectMapper registerSmartiConfigModule(ObjectMapper objectMapper) {

        SimpleModule smartiConfigModule = new SimpleModule();
        smartiConfigModule.addSerializer(io.redlink.smarti.model.config.Configuration.class, buildConfigurationSerializer(objectMapper));
        smartiConfigModule.addDeserializer(io.redlink.smarti.model.config.Configuration.class, buildConfigurationDeserializer(objectMapper));
        objectMapper.registerModule(smartiConfigModule);

        return objectMapper;
    }

    private JsonSerializer<io.redlink.smarti.model.config.Configuration > buildConfigurationSerializer(ObjectMapper objectMapper) {
        return new JsonSerializer<io.redlink.smarti.model.config.Configuration >() {
            @Override
            public void serialize(io.redlink.smarti.model.config.Configuration  value, JsonGenerator jgen, SerializerProvider provider) throws IOException {
                final TypeFactory tf = provider.getTypeFactory();
                final MapLikeType smartiConfigType = tf.constructMapLikeType(Map.class,
                        tf.constructType(String.class),
                        tf.constructCollectionLikeType(List.class,
                                ComponentConfiguration.class));
                objectMapper.writerFor(smartiConfigType).writeValue(jgen, value.getConfig());
            }
        };
    }
    private JsonDeserializer<io.redlink.smarti.model.config.Configuration > buildConfigurationDeserializer(ObjectMapper objectMapper) {
        return new JsonDeserializer<io.redlink.smarti.model.config.Configuration>() {
            @Override
            public io.redlink.smarti.model.config.Configuration deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {

                final TypeFactory tf = ctxt.getTypeFactory();
                final MapLikeType smartiConfigType = tf.constructMapLikeType(Map.class,
                        tf.constructType(String.class),
                        tf.constructCollectionLikeType(List.class,
                                ComponentConfiguration.class));

                final io.redlink.smarti.model.config.Configuration configuration = new io.redlink.smarti.model.config.Configuration();
                configuration.setConfig(objectMapper.readerFor(smartiConfigType).readValue(p, smartiConfigType));
                return configuration;
            }

        };
    }

    private class AuthContextResolver implements HandlerMethodArgumentResolver {
        @Override
        public boolean supportsParameter(MethodParameter parameter) {
            return AuthContext.class.equals(parameter.getParameterType());
        }

        @Override
        public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer, NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {

            final String token = webRequest.getHeader("X-Auth-Token");
            final Authentication authentication = SecurityContextHolder.getContext()
                    .getAuthentication();
            return new AuthContext(token, authentication);
        }
    }
}
