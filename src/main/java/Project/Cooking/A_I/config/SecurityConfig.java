package Project.Cooking.A_I.config;

import Project.Cooking.A_I.security.JwtAuthFilter;
import Project.Cooking.A_I.service.CustomUserDetailsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> {}) // ✅ picks up CorsConfigurationSource bean
                .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth

                        // ✅ preflight
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // ✅ ROOT / HEALTH (fix your 403 at base URL)
                        .requestMatchers(HttpMethod.GET, "/", "/health").permitAll()

                        // ✅ AUTH
                        .requestMatchers("/api/auth/**").permitAll()

                        // ✅ FEED (PUBLIC)
                        .requestMatchers(HttpMethod.GET, "/api/feed", "/api/feed/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/feed/**/view").permitAll()

                        // ✅ SEARCH (PUBLIC)
                        .requestMatchers(HttpMethod.GET, "/api/search", "/api/search/**").permitAll()

                        // ✅ IMPORT (PUBLIC)
                        .requestMatchers(HttpMethod.POST, "/api/import/**").permitAll()

                        // ✅ AI (PUBLIC)
                        .requestMatchers("/api/ai/**").permitAll()

                        // ✅ RECIPES (PUBLIC READ)
                        .requestMatchers(HttpMethod.GET,
                                "/api/recipes/**",
                                "/api/recipes/public/**",
                                "/api/recipes/count",
                                "/api/recipes/public-count"
                        ).permitAll()

                        // 🔐 PROTECTED
                        .requestMatchers("/api/likes/**").authenticated()
                        .requestMatchers("/api/history/**").authenticated()

                        // MISC
                        .requestMatchers("/error", "/favicon.ico").permitAll()

                        // 🔐 EVERYTHING ELSE
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable());

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        config.setAllowCredentials(true);
        config.addAllowedOriginPattern("*"); // later restrict to Vercel URL
        config.addAllowedHeader("*");
        config.addAllowedMethod("*");

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider(
            CustomUserDetailsService userDetailsService,
            PasswordEncoder passwordEncoder
    ) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }
}
