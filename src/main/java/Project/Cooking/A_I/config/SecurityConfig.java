package Project.Cooking.A_I.config;

import Project.Cooking.A_I.security.JwtAuthFilter;
import Project.Cooking.A_I.service.CustomUserDetailsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

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
                .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth

                        // ✅ PUBLIC: auth routes
                        .requestMatchers("/api/auth/**").permitAll()

                        // ✅ PUBLIC: feed browsing
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/feed", "/api/feed/**").permitAll()

                        // ✅ PUBLIC: views
                        .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/feed/*/view").permitAll()

                        // ✅ PUBLIC: misc
                        .requestMatchers("/error", "/favicon.ico").permitAll()

                        // 🔐 PROTECTED: likes need login
                        .requestMatchers("/api/likes", "/api/likes/**").authenticated()
                        .requestMatchers("/api/history/**").authenticated()
                        .requestMatchers("/api/external/**").permitAll()
                        // ✅ TEMP: allow admin seed while developing
                        .requestMatchers("/api/admin/**").permitAll()
                        // ✅ PUBLIC: recipe counts / reads
                        .requestMatchers(org.springframework.http.HttpMethod.GET,
                                "/api/recipes/count",
                                "/api/recipes/public-count",
                                "/api/recipes/**"
                        ).permitAll()
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/recipes/public/**").permitAll()
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/recipes/count").permitAll()





                        // 🔐 everything else needs login
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable());

        return http.build();
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
