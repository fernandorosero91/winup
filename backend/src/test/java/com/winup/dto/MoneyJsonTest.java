package com.winup.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class MoneyJsonTest {
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void should_serialize_wallet_balance_as_string() throws Exception {
        WalletDto wallet = new WalletDto(
                new BigDecimal("100000.00"),
                new BigDecimal("0.00"),
                "COP",
                List.of(new MovementDto(
                        UUID.randomUUID(),
                        "Saldo inicial",
                        "10:00",
                        new BigDecimal("100000.00"),
                        "INGRESO"
                ))
        );

        String json = mapper.writeValueAsString(wallet);

        assertThat(json).contains("\"balance\":\"100000.00\"");
        assertThat(json).contains("\"amount\":\"100000.00\"");
    }

    @Test
    void should_serialize_bet_money_as_string_and_keep_odds_as_number() throws Exception {
        BetDto bet = new BetDto(
                UUID.randomUUID(),
                "Liverpool vs Arsenal",
                "Resultado final",
                "Liverpool",
                new BigDecimal("50000.00"),
                new BigDecimal("2.10"),
                new BigDecimal("105000.00"),
                "PENDIENTE",
                "10:00"
        );

        String json = mapper.writeValueAsString(bet);

        assertThat(json).contains("\"stake\":\"50000.00\"");
        assertThat(json).contains("\"potential\":\"105000.00\"");
        assertThat(json).contains("\"odds\":2.10");
    }

    @Test
    void should_read_stake_from_json_string() throws Exception {
        String json = """
                {"matchId":"11111111-1111-1111-1111-111111111111",
                 "selection":"LOCAL",
                 "stake":"50000.00"}
                """;

        PlaceBetRequest request = mapper.readValue(json, PlaceBetRequest.class);

        assertThat(request.stake()).isEqualByComparingTo("50000.00");
    }
}
