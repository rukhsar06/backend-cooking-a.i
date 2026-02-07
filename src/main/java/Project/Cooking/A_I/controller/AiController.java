package Project.Cooking.A_I.controller;

import Project.Cooking.A_I.service.OpenAiService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/ai")
@CrossOrigin(origins = "*") // ✅ helps if CORS is acting up
public class AiController {

    private final OpenAiService openAiService;

    public AiController(OpenAiService openAiService) {
        this.openAiService = openAiService;
    }

    @PostMapping("/guide")
    public ResponseEntity<Map<String, Object>> guide(@RequestBody(required = false) Map<String, Object> body) {
        if (body == null) body = new HashMap<>();

        String userText = String.valueOf(body.getOrDefault("userText", "")).trim();
        String recipeTitle = String.valueOf(body.getOrDefault("recipeTitle", "")).trim();
        String contextText = String.valueOf(body.getOrDefault("contextText", "")).trim();

        Map<String, Object> out = new HashMap<>();

        if (userText.isBlank()) {
            out.put("reply", "Say that again? I didn’t catch anything.");
            return ResponseEntity.ok(out);
        }

        try {
            String reply = openAiService.getCookingGuideReply(userText, recipeTitle, contextText);
            out.put("reply", reply);
            return ResponseEntity.ok(out);
        } catch (Exception e) {
            out.put("reply", "AI error. Try again.");
            out.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(out);
        }
    }
}
