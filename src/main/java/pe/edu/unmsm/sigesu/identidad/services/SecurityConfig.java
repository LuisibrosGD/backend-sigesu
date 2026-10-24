package pe.edu.unmsm.sigesu.identidad.services;

import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.security.authentication.*;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.*;
import org.springframework.web.cors.*;
import pe.edu.unmsm.sigesu.identidad.repository.UsuarioRepository;

@Configuration
public class SecurityConfig {
  @Bean
  PasswordEncoder passwords() {
    return new BCryptPasswordEncoder(12);
  }

  @Bean
  UserDetailsService userDetails(UsuarioRepository users) {
    return email ->
        users
            .findByEmailIgnoreCase(email.trim())
            .map(
                u ->
                    User.withUsername(u.id)
                        .password(u.passwordHash)
                        .roles(u.rol)
                        .disabled(!u.activo)
                        .build())
            .orElseThrow(() -> new UsernameNotFoundException("Credenciales inválidas"));
  }

  @Bean
  AuthenticationManager authenticationManager(
      org.springframework.security.config.annotation.authentication.configuration
              .AuthenticationConfiguration
          config)
      throws Exception {
    return config.getAuthenticationManager();
  }

  @Bean
  SecurityFilterChain chain(HttpSecurity http) throws Exception {
    http.cors(c -> {})
        .csrf(c -> c.csrfTokenRepository(new HttpSessionCsrfTokenRepository()))
        .authorizeHttpRequests(
            a ->
                a.requestMatchers(
                        "/actuator/health",
                        "/api/auth/csrf",
                        "/api/auth/login",
                        "/api/auth/register",
                        "/api/public/**",
                        "/v3/api-docs/**",
                        "/swagger-ui/**",
                        "/swagger-ui.html")
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .exceptionHandling(
            e ->
                e.authenticationEntryPoint(
                        (req, res, ex) -> {
                          res.setStatus(401);
                          res.setContentType("application/json");
                          res.setCharacterEncoding("UTF-8");
                          res.getWriter().write("{\"message\":\"Sesión no activa.\"}");
                        })
                    .accessDeniedHandler(
                        (req, res, ex) -> {
                          res.setStatus(403);
                          res.setContentType("application/json");
                          res.setCharacterEncoding("UTF-8");
                          res.getWriter().write("{\"message\":\"Permiso o token CSRF inválido.\"}");
                        }))
        .requestCache(c -> c.disable())
        .formLogin(c -> c.disable())
        .httpBasic(c -> c.disable())
        .logout(c -> c.disable());
    return http.build();
  }

  @Bean
  CorsConfigurationSource cors(@Value("${app.allowed-origins}") String origins) {
    var c = new CorsConfiguration();
    c.setAllowedOrigins(Arrays.asList(origins.split(",")));
    c.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    c.setAllowedHeaders(List.of("Content-Type", "X-CSRF-TOKEN"));
    c.setAllowCredentials(true);
    var source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", c);
    return source;
  }
}
