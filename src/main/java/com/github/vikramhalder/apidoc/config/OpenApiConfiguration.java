package com.github.vikramhalder.apidoc.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.*;
import io.swagger.v3.oas.models.parameters.HeaderParameter;
import io.swagger.v3.oas.models.parameters.QueryParameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

@Configuration
public class OpenApiConfiguration {

    @Bean
    public OpenAPI openAPI() {
        final var info = new Info().title("Spring api doc customization").description("Spring api doc customization").version("1.0.0");

        final var securitySchemeForGateway = new SecurityScheme().description("schema").type(SecurityScheme.Type.OPENIDCONNECT).openIdConnectUrl("/api/well-known");
        final var components = new Components().addSecuritySchemes(SecurityScheme.Type.OPENIDCONNECT.name(), securitySchemeForGateway);
        final var securityItemForGateway = new SecurityRequirement().addList(SecurityScheme.Type.OPENIDCONNECT.name());
        return new OpenAPI().info(info).servers(List.of(new Server().url("/"))).security(List.of(securityItemForGateway)).components(components);
    }

    @Bean
    public OpenApiCustomizer openApiCustomizer() {
        return openApi -> {
            addPath(openApi);
            final var stream = openApi.getPaths().values().stream().flatMap(pathItem -> pathItem.readOperations().stream());
            final Schema<?> schema = new StringSchema().type("string")._enum(List.of("EN", "BN"));
            stream.forEach(operation -> operation.addParametersItem(new HeaderParameter().name("Accept-Language").in("header").required(true).schema(schema)));
        };
    }


    @SuppressWarnings("all")
    private void addPath(final OpenAPI openApi) {
        final List<Map<String, Object>> api = List.of(
                new HashMap<>() {{
                    this.put("tag", "Get Request");
                    this.put("path", "/api/all");
                    this.put("method", "GET");
                    this.put("content-type", "application/json");
                    this.put("properties", List.of(new HashMap<>() {{
                        this.put("name", "name");       // required
                        this.put("required", "false");  //not required
                        this.put("type", "string");     //not required
                        this.put("default", "john");    //not required
                    }}));
                }},
                new HashMap<>() {{
                    this.put("tag", "Delete Request");
                    this.put("path", "/api/delete");
                    this.put("method", "DELETE");
                    this.put("content-type", "application/json");
                    this.put("properties", List.of(new HashMap<>() {{
                        this.put("name", "id");
                        this.put("required", "false");
                        this.put("type", "int");
                        this.put("default", "0");
                    }}));
                }},
                new HashMap<>() {{
                    this.put("tag", "Post Request");
                    this.put("path", "/api/create");
                    this.put("method", "POST");
                    this.put("content-type", "application/json");
                    this.put("properties", List.of(
                            new HashMap<>() {{
                                this.put("name", "name");
                                this.put("required", "true");
                                this.put("type", "string");
                                this.put("default", "john");
                            }},
                            new HashMap<>() {{
                                this.put("name", "email");
                                this.put("required", "true");
                                this.put("type", "string");
                                this.put("default", "john@email.com");
                            }}
                    ));
                }},
                new HashMap<>() {{
                    this.put("tag", "Put Request");
                    this.put("path", "/api/update");
                    this.put("method", "POST");
                    this.put("content-type", "application/json");
                    this.put("properties", List.of(
                            new HashMap<>() {{
                                this.put("name", "id");
                                this.put("required", "true");
                                this.put("type", "int");
                                this.put("default", "0");
                            }},
                            new HashMap<>() {{
                                this.put("name", "email");
                                this.put("required", "true");
                                this.put("type", "string");
                                this.put("default", "john1@email.com");
                            }}
                    ));
                }}
        );

        try {
            for (final var item : api) {
                final Map<String, String> defaultValue = ((List<Map<String, String>>) item.get("properties"))
                        .stream()
                        .filter(m -> m.get("default") != null)
                        .collect(Collectors.toMap(m -> m.get("name"), m -> m.get("default")));
                final Map<String, Schema> properties = ((List<Map<String, String>>) item.get("properties"))
                        .stream().collect(Collectors.toMap(m -> m.get("name"), m -> new StringSchema().type(m.get("type"))));
                final List<String> requiredField = ((List<Map<String, String>>) item.get("properties"))
                        .stream()
                        .filter(m -> "true".equals(m.get("required"))).map(m -> m.get("name")).toList();


                final Schema<Object> schema = new ObjectSchema();
                schema.required(requiredField);
                schema.properties(properties);
                schema._default(defaultValue);

                final Operation operation = new Operation();
                final PathItem pathItem = new PathItem();
                if ("GET".equalsIgnoreCase(item.get("method").toString()) || "DELETE".equalsIgnoreCase(item.get("method").toString())) {
                    operation.operationId(UUID.randomUUID().toString().replace("-", ""))
                            .tags(List.of(item.get("tag") instanceof String s ? s : "Unknown"))
                            .responses(new ApiResponses());
                    properties.forEach(new BiConsumer<String, Schema>() {
                        @Override
                        public void accept(String string, Schema schema) {
                            operation.addParametersItem(new QueryParameter().name(string).schema(schema));
                        }
                    });
                } else {
                    operation.operationId(UUID.randomUUID().toString().replace("-", ""))
                            .tags(List.of(item.get("tag") instanceof String s ? s : "Unknown"))
                            .responses(new ApiResponses());
                    operation.setRequestBody(new RequestBody().content(new Content().addMediaType(item.get("content-type").toString(), new MediaType().schema(schema))));
                }
                pathItem.operation(PathItem.HttpMethod.valueOf(item.get("method").toString()), operation);
                openApi.getPaths().addPathItem(item.get("path").toString(), pathItem);
            }
        } catch (Exception ex) {
            System.out.println("Fail to create swagger api: ".concat(ex.getMessage()));
        }
    }
}
