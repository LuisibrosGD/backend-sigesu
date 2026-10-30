package pe.edu.unmsm.sigesu.identidad.services;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.session.jdbc.JdbcIndexedSessionRepository;
import org.springframework.session.config.SessionRepositoryCustomizer;

@Configuration
public class SessionJdbcConfig {
  @Bean
  SessionRepositoryCustomizer<JdbcIndexedSessionRepository> concurrentSessionAttributes() {
    // Concurrent requests may both have loaded the session before this attribute existed.
    // Keep the first persisted value; a losing CSRF token is rejected by the CSRF filter.
    return repository ->
        repository.setCreateSessionAttributeQuery(
            """
            INSERT INTO %TABLE_NAME%_ATTRIBUTES (SESSION_PRIMARY_ID, ATTRIBUTE_NAME, ATTRIBUTE_BYTES)
            VALUES (?, ?, ?)
            ON CONFLICT (SESSION_PRIMARY_ID, ATTRIBUTE_NAME) DO NOTHING
            """);
  }
}
