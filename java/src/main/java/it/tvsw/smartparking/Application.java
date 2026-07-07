package it.tvsw.smartparking;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.vaadin.flow.component.page.AppShellConfigurator;

/**
 * Avvio dell'applicazione Spring Boot + Vaadin. Lanciare con:
 * {@code mvn -f java/pom.xml spring-boot:run}, poi aprire http://localhost:8080
 */
@SpringBootApplication
public class Application implements AppShellConfigurator {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
