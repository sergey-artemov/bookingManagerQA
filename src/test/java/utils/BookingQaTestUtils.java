package utils;

import io.qameta.allure.Allure;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultHandler;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

public class BookingQaTestUtils {

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
