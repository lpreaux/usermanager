package fr.lpreaux.usermanager.infrastructure.adapter.in.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Getter;
import lombok.Setter;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

import java.util.Map;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

/**
 * Root controller providing API information and entry points.
 */
@RestController
@Tag(name = "API Information", description = "Informations générales sur l'API")
public class ApplicationController {

    /**
     * Redirige l'utilisateur vers la documentation Swagger UI quand il accède à la racine de l'application
     */
    @GetMapping("/")
    @Operation(hidden = true) // Cache cette opération dans la documentation OpenAPI
    public ModelAndView redirectToSwaggerUI() {
        return new ModelAndView("redirect:/swagger-ui/index.html");
    }

    @GetMapping("/api/v1")
    @Operation(
            summary = "Informations sur l'API",
            description = "Fournit des informations sur l'API et des liens vers les principales ressources"
    )
    @ApiResponse(
            responseCode = "200",
            description = "Informations récupérées avec succès",
            content = @Content(mediaType = "application/json")
    )
    public ResponseEntity<EntityModel<Map<String, Object>>> getApiInfo() {
        Map<String, Object> apiInfo = Map.of(
                "name", "User Manager API",
                "version", "1.0",
                "description", "API for managing users with hexagonal architecture"
        );

        EntityModel<Map<String, Object>> resource = EntityModel.of(apiInfo);
        resource.add(linkTo(methodOn(UserController.class).getAllUsers()).withRel("users"));
        resource.add(linkTo(methodOn(ApplicationController.class).getApiInfo()).withSelfRel());
        resource.add(linkTo(methodOn(ApplicationController.class).healthCheck()).withRel("health"));

        return ResponseEntity.ok(resource);
    }

    @GetMapping("/health")
    @Operation(
            summary = "Vérification de l'état",
            description = "Point de terminaison pour vérifier l'état de l'API"
    )
    @ApiResponse(
            responseCode = "200",
            description = "API fonctionnelle",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = HealthResponse.class))
    )
    public ResponseEntity<Map<String, String>> healthCheck() {
        return ResponseEntity.ok(Map.of("status", "UP"));
    }

    /**
     * Schéma pour la réponse de santé.
     */
    @Setter
    @Getter
    private static class HealthResponse {
        public String status;

    }
}
