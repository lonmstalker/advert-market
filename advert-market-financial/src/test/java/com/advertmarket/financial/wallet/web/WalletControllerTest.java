package com.advertmarket.financial.wallet.web;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.advertmarket.financial.api.model.LedgerEntry;
import com.advertmarket.financial.api.model.WalletSummary;
import com.advertmarket.financial.api.port.WalletPort;
import com.advertmarket.shared.model.AccountId;
import com.advertmarket.shared.model.EntryType;
import com.advertmarket.shared.model.UserId;
import com.advertmarket.shared.pagination.CursorPage;
import com.advertmarket.shared.security.PrincipalAuthentication;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@DisplayName("WalletController — /api/v1/wallet endpoints")
@ExtendWith(MockitoExtension.class)
class WalletControllerTest {

    private static final long USER_ID = 42L;

    private MockMvc mockMvc;

    @Mock
    private WalletPort walletPort;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new WalletController(walletPort))
                .build();

        SecurityContextHolder.getContext().setAuthentication(testAuth());
    }

    @Test
    @DisplayName("GET /api/v1/wallet/summary returns wallet balances")
    void getSummary() throws Exception {
        when(walletPort.getSummary(new UserId(USER_ID)))
                .thenReturn(new WalletSummary(
                        1_000_000_000L, 5_000_000_000L, 10_000_000_000L));

        mockMvc.perform(get("/api/v1/wallet/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pendingBalanceNano").value(1_000_000_000L))
                .andExpect(jsonPath("$.availableBalanceNano").value(5_000_000_000L))
                .andExpect(jsonPath("$.totalEarnedNano").value(10_000_000_000L));
    }

    @Test
    @DisplayName("GET /api/v1/wallet/transactions returns paginated entries")
    void getTransactions() throws Exception {
        var entry = new LedgerEntry(
                1L, null, AccountId.ownerPending(new UserId(USER_ID)),
                EntryType.ESCROW_RELEASE, 0L, 1_000_000_000L,
                "idem-1", UUID.randomUUID(), "Release", Instant.now());
        when(walletPort.getTransactions(new UserId(USER_ID), null, 20))
                .thenReturn(new CursorPage<>(List.of(entry), "next-cursor"));

        mockMvc.perform(get("/api/v1/wallet/transactions")
                        .param("limit", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.nextCursor").value("next-cursor"))
                .andExpect(jsonPath("$.hasNext").value(true));
    }

    @Test
    @DisplayName("GET /api/v1/wallet/transactions uses default limit")
    void getTransactionsDefaultLimit() throws Exception {
        when(walletPort.getTransactions(new UserId(USER_ID), null, 20))
                .thenReturn(CursorPage.empty());

        mockMvc.perform(get("/api/v1/wallet/transactions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isEmpty());
    }

    private static PrincipalAuthentication testAuth() {
        return new PrincipalAuthentication() {
            @Override
            public UserId getUserId() {
                return new UserId(USER_ID);
            }

            @Override
            public String getJti() {
                return "test-jti";
            }

            @Override
            public boolean isOperator() {
                return false;
            }

            @Override
            public long getTokenExpSeconds() {
                return 0;
            }

            @Override
            public Collection<? extends GrantedAuthority> getAuthorities() {
                return List.of();
            }

            @Override
            public Object getCredentials() {
                return null;
            }

            @Override
            public Object getDetails() {
                return null;
            }

            @Override
            public Object getPrincipal() {
                return getUserId();
            }

            @Override
            public boolean isAuthenticated() {
                return true;
            }

            @Override
            public void setAuthenticated(boolean isAuthenticated) {
            }

            @Override
            public String getName() {
                return String.valueOf(USER_ID);
            }
        };
    }
}
