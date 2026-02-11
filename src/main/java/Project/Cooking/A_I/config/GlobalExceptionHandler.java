package Project.Cooking.A_I.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(Throwable.class)
    public ResponseEntity<?> handle(Throwable e) {
        // ✅ This MUST print the real exception + Caused by to Render logs
        log.error("=== REAL ERROR ===", e);

        // ✅ This makes the frontend see JSON instead of HTML error page
        return ResponseEntity.status(500).body(Map.of(
                "error", e.getClass().getName(),
                "message", String.valueOf(e.getMessage())
        ));
    }
}
