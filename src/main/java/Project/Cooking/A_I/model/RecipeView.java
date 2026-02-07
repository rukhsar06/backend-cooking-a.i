package Project.Cooking.A_I.model;

import jakarta.persistence.*;

@Entity
@Table(
        name = "recipe_views",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "recipe_id"})
)
public class RecipeView {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(optional = false)
    @JoinColumn(name = "recipe_id")
    private Recipe recipe;

    public Long getId() { return id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public Recipe getRecipe() { return recipe; }
    public void setRecipe(Recipe recipe) { this.recipe = recipe; }
}
