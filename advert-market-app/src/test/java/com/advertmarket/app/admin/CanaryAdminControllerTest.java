package com.advertmarket.app.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.advertmarket.communication.canary.CanaryRouter;
import com.advertmarket.shared.deploy.CanaryProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

@DisplayName("CanaryAdminController")
class CanaryAdminControllerTest {

    private static final String TOKEN = "secret-admin-token";

    private CanaryRouter canaryRouter;
    private CanaryAdminController controller;

    @BeforeEach
    void setUp() {
        canaryRouter = mock(CanaryRouter.class);
        controller = new CanaryAdminController(
                canaryRouter, new CanaryProperties(TOKEN));
    }

    @Nested
    @DisplayName("getCanary")
    class GetCanary {

        @Test
        @DisplayName("Should return canary status with valid token")
        void validToken_returnsStatus() {
            when(canaryRouter.getCanaryPercent()).thenReturn(25);
            when(canaryRouter.getSalt()).thenReturn("test-salt");

            var response = controller.getCanary("Bearer " + TOKEN);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().percent()).isEqualTo(25);
            assertThat(response.getBody().salt()).isEqualTo("test-salt");
        }

        @Test
        @DisplayName("Should return 401 with invalid token")
        void invalidToken_returns401() {
            var response = controller.getCanary("Bearer wrong-token");

            assertThat(response.getStatusCode())
                    .isEqualTo(HttpStatus.UNAUTHORIZED);
            assertThat(response.getBody()).isNull();
        }

        @Test
        @DisplayName("Should return 401 with missing Bearer prefix")
        void missingBearer_returns401() {
            var response = controller.getCanary(TOKEN);

            assertThat(response.getStatusCode())
                    .isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @Test
        @DisplayName("Should return 401 when token is not configured")
        void noTokenConfigured_returns401() {
            var controllerNoToken = new CanaryAdminController(
                    canaryRouter, new CanaryProperties(null));

            var response = controllerNoToken.getCanary(
                    "Bearer " + TOKEN);

            assertThat(response.getStatusCode())
                    .isEqualTo(HttpStatus.UNAUTHORIZED);
        }
    }

    @Nested
    @DisplayName("setCanary")
    class SetCanary {

        @Test
        @DisplayName("Should update percent with valid token")
        void updatesPercent() {
            when(canaryRouter.getCanaryPercent()).thenReturn(50);
            when(canaryRouter.getSalt()).thenReturn("salt");

            var response = controller.setCanary(
                    "Bearer " + TOKEN,
                    new CanaryAdminController.CanaryUpdate(50, null));

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            verify(canaryRouter).setCanaryPercent(50);
            verify(canaryRouter, never()).setSalt(null);
        }

        @Test
        @DisplayName("Should update salt with valid token")
        void updatesSalt() {
            when(canaryRouter.getCanaryPercent()).thenReturn(0);
            when(canaryRouter.getSalt()).thenReturn("new-salt");

            var response = controller.setCanary(
                    "Bearer " + TOKEN,
                    new CanaryAdminController.CanaryUpdate(
                            null, "new-salt"));

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            verify(canaryRouter).setSalt("new-salt");
            verify(canaryRouter, never()).setCanaryPercent(0);
        }

        @Test
        @DisplayName("Should return 401 with invalid token")
        void invalidToken_returns401() {
            var response = controller.setCanary(
                    "Bearer wrong",
                    new CanaryAdminController.CanaryUpdate(50, null));

            assertThat(response.getStatusCode())
                    .isEqualTo(HttpStatus.UNAUTHORIZED);
            verify(canaryRouter, never()).setCanaryPercent(50);
        }
    }
}
