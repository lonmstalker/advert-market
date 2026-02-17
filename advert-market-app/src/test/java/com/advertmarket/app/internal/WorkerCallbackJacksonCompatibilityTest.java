package com.advertmarket.app.internal;

import static org.assertj.core.api.Assertions.assertThatCode;

import com.advertmarket.shared.event.WorkerCallback;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

@DisplayName("WorkerCallback Jackson compatibility")
class WorkerCallbackJacksonCompatibilityTest {

    @Test
    @DisplayName("Deserializes callback payload with Spring Jackson mapper")
    void deserializesWithSpringJacksonMapper() {
        var mapper = new ObjectMapper();
        var body = """
                {
                  "callbackType":"DEPOSIT_CONFIRMED",
                  "dealId":"9e7e18c0-4ea0-44cc-bff6-df7f1f8990c7",
                  "correlationId":"%s",
                  "payload":{
                    "txHash":"dep-tx",
                    "amountNano":1500000000,
                    "expectedAmountNano":1500000000,
                    "confirmations":3,
                    "fromAddress":"0QCQRtZOX1F_sbos8P6-AVgcx8sqZ35dnrj_o7zvDQ3yggz4",
                    "depositAddress":"0QDP4YrFGiTaR4n7ONCEQZ-sv71SZTfWmVi9QiYmsdPiB5fM"
                  }
                }
                """.formatted(UUID.randomUUID());

        assertThatCode(() -> mapper.readValue(body, WorkerCallback.class))
                .doesNotThrowAnyException();
    }
}
