package Project.Cooking.A_I.config;

import Project.Cooking.A_I.repository.RecipeRepository;
import Project.Cooking.A_I.service.MealDbImportService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MealDbBootstrap {

    @Bean
    CommandLineRunner seedMeals(RecipeRepository recipeRepo, MealDbImportService importService) {
        return args -> {
            long publicCount = recipeRepo.countByIsPublicTrue();
            if (publicCount < 20) {
                importService.importMeals(60);
            }
        };
    }
}
