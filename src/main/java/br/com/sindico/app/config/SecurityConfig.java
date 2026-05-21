package br.com.sindico.app.config;

import br.com.sindico.app.security.SindicoLoginSuccessHandler;
import br.com.sindico.app.security.ApiBearerEnforcementFilter;
import br.com.sindico.app.security.JwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Arrays;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
public class SecurityConfig {

    private static final String DEV_JWT_DEFAULT = "dev-only-change-this-secret-dev-only-change-this-secret";

    private final Environment environment;

    public SecurityConfig(Environment environment) {
        this.environment = environment;
    }

    private boolean isStrictProfileActive() {
        return Arrays.stream(environment.getActiveProfiles())
                .anyMatch(profile -> "prod".equals(profile) || "supabase".equals(profile));
    }

    private void validateSecurityRequirements(String allowedOrigins, String publicBaseUrl) {
        if (!isStrictProfileActive()) {
            return;
        }

        String jwtSecret = environment.getProperty("app.jwt.secret", "");
        if (jwtSecret.isBlank() || DEV_JWT_DEFAULT.equals(jwtSecret)) {
            throw new IllegalStateException("APP_JWT_SECRET obrigatorio e seguro em supabase/prod.");
        }

        if (allowedOrigins == null || allowedOrigins.isBlank()) {
            throw new IllegalStateException("APP_CORS_ORIGINS obrigatorio em supabase/prod.");
        }

        if (publicBaseUrl == null || publicBaseUrl.isBlank()) {
            throw new IllegalStateException("APP_PUBLIC_BASE_URL obrigatorio em supabase/prod.");
        }
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${app.cors.allowed-origins:}") String allowedOrigins,
            @Value("${app.public-base-url:}") String publicBaseUrl) {
        validateSecurityRequirements(allowedOrigins, publicBaseUrl);

        CorsConfiguration configuration = new CorsConfiguration();
        java.util.List<String> origins = java.util.Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
        if (origins.isEmpty()) {
            // Fallback apenas para ambiente local.
            configuration.setAllowedOriginPatterns(java.util.List.of("*"));
            configuration.setAllowCredentials(false);
        } else {
            configuration.setAllowedOrigins(origins);
            // allowCredentials=true exige origens explícitas (incompatível com padrão "*").
            configuration.setAllowCredentials(true);
        }
        configuration.setAllowedMethods(java.util.List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(java.util.List.of("*"));
        configuration.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }

    @Bean
    public SindicoLoginSuccessHandler sindicoLoginSuccessHandler() {
        return new SindicoLoginSuccessHandler();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            SindicoLoginSuccessHandler loginSuccessHandler,
            ApiBearerEnforcementFilter apiBearerEnforcementFilter,
            JwtAuthenticationFilter jwtAuthenticationFilter)
            throws Exception {
        return http
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf.ignoringRequestMatchers("/api/**"))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(apiBearerEnforcementFilter, JwtAuthenticationFilter.class)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/login", "/cadastro",
                                "/termos", "/privacidade", "/cookies",
                                "/esqueci-senha", "/redefinir-senha",
                                "/css/**", "/js/**", "/error",
                                "/api/auth/login", "/api/auth/register",
                                "/actuator/health").permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/ia/config").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/ia/config/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/ia/config").hasRole("ADMIN")
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().authenticated())
                .exceptionHandling(ex -> ex
                        .defaultAuthenticationEntryPointFor(
                                (request, response, authException) -> {
                                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                                    response.setContentType("application/json;charset=UTF-8");
                                    response.getWriter().write("{\"error\":\"Nao autenticado\",\"status\":401}");
                                },
                                request -> request.getRequestURI().startsWith("/api/")))
                .formLogin(form -> form
                        .loginPage("/login")
                        .failureUrl("/login?error")
                        .successHandler(loginSuccessHandler))
                .logout(logout -> logout
                        .logoutSuccessUrl("/login?logout")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID"))
                .sessionManagement(session -> session
                        .maximumSessions(1)
                        .maxSessionsPreventsLogin(false))
                .headers(headers -> headers
                        .contentSecurityPolicy(csp -> csp
                                .policyDirectives("default-src 'self'; style-src 'self' 'unsafe-inline'; img-src 'self' data:; frame-ancestors 'none'"))
                        .referrerPolicy(referrer -> referrer
                                .policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                        .frameOptions(frame -> frame.deny())
                        .contentTypeOptions(Customizer.withDefaults()))
                .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
