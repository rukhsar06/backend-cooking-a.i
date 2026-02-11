package Project.Cooking.A_I.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ServerErrorLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(ServerErrorLoggingFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        try {
            chain.doFilter(req, res);
        } catch (Throwable t) {
            log.error("=== REAL SERVER ERROR === {} {} | authHeaderPresent={} | msg={}",
                    req.getMethod(),
                    req.getRequestURI(),
                    req.getHeader("Authorization") != null,
                    t.getMessage(),
                    t
            );
            throw t; // keep behavior same, but now logs show cause
        }
    }
}
