package co.edu.unal.ares;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/** La aplicación completa arranca: Spring, Flyway, JPA y Thymeleaf configurados sin errores. */
@SpringBootTest
@ActiveProfiles("test")
class AresApplicationTests {

    @Test
    void contextLoads() {
    }
}
