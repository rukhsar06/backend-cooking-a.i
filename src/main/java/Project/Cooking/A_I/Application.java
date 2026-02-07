package Project.Cooking.A_I;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@SpringBootApplication
public class Application {

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}

	// ✅ Add this to allow frontend requests from React
	@Bean
	public WebMvcConfigurer corsConfigurer() {
		return new WebMvcConfigurer() {
			@Override
			public void addCorsMappings(CorsRegistry registry) {
				registry.addMapping("/**") // all endpoints
						.allowedOrigins("http://localhost:3000") // React app
						.allowedMethods("GET","POST","PUT","DELETE") // optional: limit methods
						.allowedHeaders("*"); // optional: allow all headers
			}
		};
	}
}
