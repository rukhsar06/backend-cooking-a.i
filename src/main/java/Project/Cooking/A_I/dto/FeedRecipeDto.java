package Project.Cooking.A_I.dto;

import java.time.LocalDateTime;

public record FeedRecipeDto(
        Long id,
        String title,
        String imageUrl,
        String tags,
        long likes,
        boolean likedByMe,
        long views,
        String source,
        LocalDateTime createdAt
) {}
