package Project.Cooking.A_I.config;

import Project.Cooking.A_I.model.Recipe;
import Project.Cooking.A_I.model.User;
import Project.Cooking.A_I.repository.RecipeRepository;
import Project.Cooking.A_I.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RecipeSeed {

    @Bean
    CommandLineRunner seedPublicRecipes(RecipeRepository recipeRepository, UserRepository userRepository) {
        return args -> {
            // ✅ Don’t reseed if you already have public recipes
            if (recipeRepository.countByIsPublicTrue() > 0) return;

            // ✅ Find or create a "system user" to satisfy NOT NULL user_id
            User systemUser = userRepository.findByEmail("system@cooking.ai")
                    .orElseGet(() -> {
                        User u = new User();
                        u.setEmail("system@cooking.ai");
                        u.setUsername("CookingAI");
                        u.setPassword("DONT_LOGIN_WITH_THIS"); // doesn't matter if you never login with it
                        return userRepository.save(u);
                    });

            recipeRepository.save(makePublic(
                    systemUser,
                    "15-min Garlic Noodles",
                    "Noodles, garlic, soy sauce, chili flakes, spring onion",
                    "1) Boil noodles\n2) Saute garlic\n3) Add sauces\n4) Toss noodles\n5) Top with spring onion",
                    "veg,quick,spicy",
                    "https://images.unsplash.com/photo-1604908177522-3b8f6d5e2c1f?auto=format&fit=crop&w=800&q=80"
            ));

            recipeRepository.save(makePublic(
                    systemUser,
                    "High-Protein Paneer Bowl",
                    "Paneer, curd, cucumber, onion, spices",
                    "1) Cube paneer\n2) Mix curd + spices\n3) Toss veggies\n4) Combine and serve",
                    "high-protein,indian,veg",
                    "https://images.unsplash.com/photo-1604908554047-2b79b4d343ec?auto=format&fit=crop&w=800&q=80"
            ));

            recipeRepository.save(makePublic(
                    systemUser,
                    "Chocolate Oats Overnight",
                    "Oats, milk, cocoa, honey, banana",
                    "1) Mix oats + milk + cocoa\n2) Refrigerate overnight\n3) Add banana and honey",
                    "breakfast,healthy,quick",
                    "https://images.unsplash.com/photo-1517673400267-0251440c45dc?auto=format&fit=crop&w=800&q=80"
            ));
        };
    }

    private Recipe makePublic(User systemUser, String title, String ingredients, String steps, String tags, String imageUrl) {
        Recipe r = new Recipe();
        r.setTitle(title);
        r.setIngredients(ingredients);
        r.setSteps(steps);
        r.setTags(tags);
        r.setImageUrl(imageUrl);

        r.setPublic(true);
        r.setSource("CURATED");
        r.setLikes(0);
        r.setViews(0);

        r.setUser(systemUser); // ✅ prevents user_id null crash
        return r;
    }
}
