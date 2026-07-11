package it.tvsw.smartparking.ui;

import java.time.Duration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.WebDriverWait;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test UI opzionale con Selenium. Tag "ui": ESCLUSO dalla build standard
 * ({@code mvn verify}), vedi configurazione surefire in pom.xml.
 *
 * <p>Prerequisiti per eseguirlo manualmente:
 * <ol>
 *   <li>Avviare l'applicazione: {@code mvn -f java/pom.xml spring-boot:run}</li>
 *   <li>Avere Chrome + ChromeDriver disponibili nel PATH</li>
 *   <li>Eseguire: {@code mvn -f java/pom.xml test -Dtest.groups=ui -Dtest.excludedGroups=}</li>
 * </ol>
 */
@Tag("ui")
class UISeleniumTest {

    private WebDriver driver;

    @BeforeEach
    void avviaBrowser() {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless=new");
        driver = new ChromeDriver(options);
    }

    @AfterEach
    void chiudiBrowser() {
        if (driver != null) {
            driver.quit();
        }
    }

    @Test
    void laPaginaSiCaricaEIlBottoneArrivaStdFunziona() {
        driver.get("http://localhost:8080/");

        // Vaadin renderizza la pagina lato client: serve un'attesa esplicita
        // finche' il DOM applicativo non e' pronto (al primo avvio il dev-bundle
        // puo' richiedere diversi secondi).
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));
        wait.until(d -> d.getPageSource().contains("SmartParking"));
        assertTrue(driver.getPageSource().contains("SmartParking"));

        driver.findElement(By.xpath("//vaadin-button[contains(., 'Arriva auto STD')]")).click();

        // Anche l'aggiornamento delle etichette passa da un round-trip
        // client-server: attesa esplicita sul nuovo valore dello stato.
        wait.until(d -> {
            String s = d.findElement(By.xpath("//span[contains(., 'Stato:')]")).getText();
            return s.contains("INGR") || s.contains("NEG");
        });
        String statoVisibile = driver.findElement(By.xpath("//span[contains(., 'Stato:')]")).getText();
        assertTrue(statoVisibile.contains("INGR") || statoVisibile.contains("NEG"));
    }
}
