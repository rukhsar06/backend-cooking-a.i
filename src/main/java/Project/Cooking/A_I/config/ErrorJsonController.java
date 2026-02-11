package Project.Cooking.A_I.config;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class ErrorJsonController implements ErrorController {

    @RequestMapping("/error")
    public ResponseEntity<?> error(HttpServletRequest req) {
        Object status = req.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        Object msg = req.getAttribute(RequestDispatcher.ERROR_MESSAGE);
        Object ex  = req.getAttribute(RequestDispatcher.ERROR_EXCEPTION);

        return ResponseEntity.status(status == null ? 500 : Integer.parseInt(status.toString()))
                .body(Map.of(
                        "status", status,
                        "message", msg,
                        "exception", ex == null ? null : ex.getClass().getName()
                ));
    }
}
