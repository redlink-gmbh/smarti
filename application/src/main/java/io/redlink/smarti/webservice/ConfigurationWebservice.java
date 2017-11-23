package io.redlink.smarti.webservice;

import io.redlink.smarti.model.config.ComponentConfiguration;
import io.redlink.smarti.model.config.Configuration;
import io.redlink.smarti.services.AuthenticationService;
import io.redlink.smarti.services.ConfigurationService;
import io.redlink.smarti.webservice.pojo.AuthContext;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@CrossOrigin
@RestController
@RequestMapping(value = "/config",
        produces = MimeTypeUtils.APPLICATION_JSON_VALUE)
@Api
public class ConfigurationWebservice {

    private final ConfigurationService configService;
    private final AuthenticationService authenticationService;

    public ConfigurationWebservice(ConfigurationService configService, AuthenticationService authenticationService) {
        this.configService = configService;
        this.authenticationService = authenticationService;
    }

    @ApiOperation(value = "retrieve list of the basic configurations", response = ComponentConfiguration.class, responseContainer ="{'category': [..]}")
    @RequestMapping(method = RequestMethod.GET)
    public ResponseEntity<Configuration> getConfigurationComponents(AuthContext authContext) throws IOException {
        authenticationService.assertAuthenticated(authContext);
        return ResponseEntity.ok(configService.getDefaultConfiguration());
    }

}
