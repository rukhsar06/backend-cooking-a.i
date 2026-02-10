package Project.Cooking.A_I.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "recipes")
public class Recipe {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    // 🔥 CRITICAL FIX: LAZY LOBs (Postgres-safe)
    @Lob
    @Basic(fetch = FetchType.LAZY)
    @Column(nullable = false, columnDefinition = "TEXT")
    private String ingredients;

    @Lob
    @Basic(fetch = FetchType.LAZY)
    @Column(nullable = false, columnDefinition = "TEXT")
    private String steps;

    @Column(nullable = false)
    private boolean isPublic = false;

    @Column(nullable = false)
    private String source = "USER";

    @Column(columnDefinition = "TEXT")
    private String imageUrl;

    @Column(columnDefinition = "TEXT")
    private String tags;

    @Column(nullable = false)
    private long likes = 0;

    @Column(nullable = false)
    private long views = 0;

    @Column
    private String externalId;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    // Getters & Setters
    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getIngredients() { return ingredients; }
    public String getSteps() { return steps; }
    public boolean isPublic() { return isPublic; }
    public String getSource() { return source; }
    public String getImageUrl() { return imageUrl; }
    public String getTags() { return tags; }
    public long getLikes() { return likes; }
    public long getViews() { return views; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public User getUser() { return user; }
    public String getExternalId() { return externalId; }

    public void setId(Long id) { this.id = id; }
    public void setTitle(String title) { this.title = title; }
    public void setIngredients(String ingredients) { this.ingredients = ingredients; }
    public void setSteps(String steps) { this.steps = steps; }
    public void setPublic(boolean aPublic) { isPublic = aPublic; }
    public void setSource(String source) { this.source = source; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public void setTags(String tags) { this.tags = tags; }
    public void setLikes(long likes) { this.likes = likes; }
    public void setViews(long views) { this.views = views; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public void setUser(User user) { this.user = user; }
    public void setExternalId(String externalId) { this.externalId = externalId; }
}
