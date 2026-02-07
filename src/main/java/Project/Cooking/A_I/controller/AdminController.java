package Project.Cooking.A_I.controller;

import Project.Cooking.A_I.service.MealDbImportService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final MealDbImportService mealDbImportService;

    public AdminController(MealDbImportService mealDbImportService) {
        this.mealDbImportService = mealDbImportService;
    }

    // GET /api/admin/seed?count=50
    @GetMapping("/seed")
    public ResponseEntity<?> seed(@RequestParam(defaultValue = "50") int count) {
        if (count < 1) count = 1;
        if (count > 200) count = 200;

        int inserted = mealDbImportService.importMoreMeals(count);

        return ResponseEntity.ok(Map.of(
                "requested", count,
                "inserted", inserted
        ));
    }
}
