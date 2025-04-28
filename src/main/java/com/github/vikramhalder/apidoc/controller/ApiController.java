package com.github.vikramhalder.apidoc.controller;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
public class ApiController {

    @Value("${server.port}")
    private String serverPort;


    @GetMapping("/")
    public String index() {
        return "Running ....";
    }

    @Hidden
    @GetMapping(value = "/api/well-known", produces = MediaType.APPLICATION_JSON_VALUE)
    public String wellKnown() {
        final String openIdUrl = "http://localhost:".concat(serverPort);
        return """
                {
                  "issuer": "{origin}",
                  "jwks_uri": "{origin}/api/auth/jwks",
                  "token_endpoint": "{origin}/api/auth/login",
                  "revocation_endpoint": "{origin}/api/auth/revoke",
                  "introspection_endpoint": "{origin}/api/auth/introspect",
                  "authorization_endpoint": "{origin}/api/auth/authorize",
                  "token_endpoint_auth_methods_supported": [
                    "client_secret_post"
                  ],
                  "response_types_supported": [
                    "password"
                  ],
                  "grant_types_supported": [
                    "password"
                  ],
                  "revocation_endpoint_auth_methods_supported": [
                    "client_secret_post"
                  ],
                  "introspection_endpoint_auth_methods_supported": [
                    "client_secret_post"
                  ],
                  "code_challenge_methods_supported": [
                    "S256"
                  ]
                }
                """
                .replace("{origin}", openIdUrl);
    }

    @PostMapping(value = "/api/auth/login", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public LoginResponse login() {
        final var response = new LoginResponse();
        response.setAccessToken(UUID.randomUUID().toString());
        return response;
    }


    public static class LoginResponse {
        @JsonProperty("access_token")
        private String accessToken;

        public void setAccessToken(String accessToken) {
            this.accessToken = accessToken;
        }
    }
}