package utils;

import io.qameta.allure.Allure;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

public class BookingQaTestUtils {

    public static void writeEnvironment() {

        Map<String, String> values = new LinkedHashMap<>();
        values.put("Java", System.getProperty("java.version"));
        values.put("OS", System.getProperty("os.name") + " " + System.getProperty("os.version"));
        values.put("Database", "PostgreSQL (Testcontainers)");

        Properties props = new Properties();
        props.putAll(values);

        Path resultsDir = Paths.get("target", "allure-results");
        try {
            Files.createDirectories(resultsDir);
            try (OutputStream os =
                         Files.newOutputStream(resultsDir.resolve("environment.properties"))) {
                props.store(os, null);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Не удалось записать environment.properties", e);
        }
    }

    public static class AllureResultHandler implements ResultHandler {
        @Override
        public void handle(MvcResult result) throws UnsupportedEncodingException {
            MockHttpServletRequest request = result.getRequest();
            MockHttpServletResponse response = result.getResponse();

            // Запрос: прикладываем, только если есть тело (POST/PUT/PATCH)
            String requestBody = request.getContentAsString();
            if (requestBody != null && !requestBody.isBlank()) {
                Allure.attachment("Request body", requestBody);
            }

            // Метод и URI полезны всегда — даже для GET
            Allure.attachment("Request",
                    request.getMethod() + " " + request.getRequestURI());

            // Ответ: тело + статус
            String responseBody;
            try {
                responseBody = response.getContentAsString(StandardCharsets.UTF_8);
            } catch (Exception e) {
                responseBody = "<не удалось прочитать: " + e.getMessage() + ">";
            }
            Allure.attachment("Response " + response.getStatus(), responseBody);
        }
    }

}
