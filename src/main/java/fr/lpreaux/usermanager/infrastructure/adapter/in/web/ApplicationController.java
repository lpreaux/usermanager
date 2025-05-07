package fr.lpreaux.usermanager.infrastructure.adapter.in.web;

import org.springframework.hateoas.EntityModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

/**
 * Root controller providing API information and entry points.
 */
@RestController
public class ApplicationController {

    @GetMapping("/api/v1")
    public ResponseEntity<EntityModel<Map<String, Object>>> getApiInfo() {
        Map<String, Object> apiInfo = Map.of(
                "name", "User Manager API",
                "version", "1.0",
                "description", "API for managing users with hexagonal architecture"
        );

        EntityModel<Map<String, Object>> resource = EntityModel.of(apiInfo);
        resource.add(linkTo(methodOn(UserController.class).getAllUsers()).withRel("users"));
        resource.add(linkTo(methodOn(ApplicationController.class).getApiInfo()).withSelfRel());

        return ResponseEntity.ok(resource);
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> healthCheck() {
        return ResponseEntity.ok(Map.of("status", "UP"));
    }
}
