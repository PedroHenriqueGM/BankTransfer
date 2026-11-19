package com.pedro.banktransfer;
import com.pedro.banktransfer.entity.ContaEntity;
import com.pedro.banktransfer.repository.ContaRepository;
import com.pedro.banktransfer.service.ContaService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;

import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Testcontainers
class ContaServiceIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres =
            new PostgreSQLContainer("postgres:17-alpine");

    @Autowired
    private ContaService contaService;

    @Autowired
    private ContaRepository contaRepository;

    @Test
    void deveCriarContaComSaldoInicial() {
        ContaEntity conta = contaService.criarConta(
                "Pedro Teste",
                new BigDecimal("1000.50")
        );

        ContaEntity salva = contaRepository
                .findById(conta.getId())
                .orElseThrow();

        assertEquals(
                0,
                new BigDecimal("1000.50").compareTo(salva.getSaldo())
        );
    }

    @Test
    void deveCriarContaComSaldoZeroQuandoOmitido() {
        ContaEntity conta = contaService.criarConta(
                "João Teste",
                null
        );

        ContaEntity salva = contaRepository
                .findById(conta.getId())
                .orElseThrow();

        assertEquals(
                0,
                BigDecimal.ZERO.compareTo(salva.getSaldo())
        );
    }

    @Test
    void deveRejeitarSaldoNegativo() {
        assertThrows(
                IllegalArgumentException.class,
                () -> contaService.criarConta(
                        "Maria Teste",
                        new BigDecimal("-100.00")
                )
        );
    }

    @Test
    void deveRejeitarMaisDeDuasCasasDecimais() {
        assertThrows(
                IllegalArgumentException.class,
                () -> contaService.criarConta(
                        "Ana Teste",
                        new BigDecimal("100.999")
                )
        );
    }
}
