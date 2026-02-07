package Project.Cooking.A_I.controller;

import Project.Cooking.A_I.service.MealDbImportService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/external")
public class ExternalRecipeController {

    private final MealDbImportService importService;

    public ExternalRecipeController(MealDbImportService importService) {
        this.importService = importService;
    }

    // GET /api/external/sync?limit=40
    @GetMapping("/sync")
    public ResponseEntity<?> sync(@RequestParam(defaultValue = "40") int limit) {
        int saved = importService.importMeals(limit);
        return ResponseEntity.ok(Map.of("saved", saved, "source", "MEALDB"));
    }
}
