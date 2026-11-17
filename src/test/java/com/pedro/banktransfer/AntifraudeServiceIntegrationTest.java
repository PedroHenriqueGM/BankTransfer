package com.pedro.banktransfer;

import com.pedro.banktransfer.enums.NivelRisco;
import com.pedro.banktransfer.enums.TipoAlertaFraude;
import com.pedro.banktransfer.service.AntifraudeService;
import com.pedro.banktransfer.service.ResultadoAnaliseRisco;
import com.pedro.banktransfer.service.ResultadoTransferencia;
import com.pedro.banktransfer.service.TransferenciaService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Testcontainers
public class AntifraudeServiceIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres =
            new PostgreSQLContainer("postgres:17-alpine");

    @Autowired
    private AntifraudeService antifraudeService;

    @Autowired
    private TransferenciaService transferenciaService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void deveClassificarTransferenciaNormalComoRiscoBaixo() {

        Long origemId = jdbcTemplate.queryForObject(
                """
                INSERT INTO conta_entity (titular, saldo)
                VALUES ('Pedro Teste', 1000.00)
                RETURNING id
                """,
                Long.class
        );

        Long destinoId = jdbcTemplate.queryForObject(
                """
                INSERT INTO conta_entity (titular, saldo)
                VALUES ('João Teste', 500.00)
                RETURNING id
                """,
                Long.class
        );

        ResultadoTransferencia resultado =
                transferenciaService.transferir(
                        origemId,
                        destinoId,
                        new BigDecimal("100.00"),
                        UUID.randomUUID().toString()
                );

        ResultadoAnaliseRisco analise =
                antifraudeService.avaliarRisco(
                        resultado.transferencia()
                );

        assertEquals(0, analise.pontuacao());
        assertEquals(NivelRisco.BAIXO, analise.nivel());
        assertTrue(analise.regrasAcionadas().isEmpty());
    }

    @Test
    void deveClassificarValorElevadoComoRiscoModerado() {

        Long origemId = jdbcTemplate.queryForObject(
                """
                INSERT INTO conta_entity (titular, saldo)
                VALUES ('Maria Teste', 10000.00)
                RETURNING id
                """,
                Long.class
        );

        Long destinoId = jdbcTemplate.queryForObject(
                """
                INSERT INTO conta_entity (titular, saldo)
                VALUES ('Ana Teste', 500.00)
                RETURNING id
                """,
                Long.class
        );

        ResultadoTransferencia resultado = transferenciaService.transferir(
                origemId,
                destinoId,
                new BigDecimal("6000.00"),
                UUID.randomUUID().toString()
        );

        ResultadoAnaliseRisco analise =
                antifraudeService.avaliarRisco(resultado.transferencia());

        assertEquals(35, analise.pontuacao());
        assertEquals(NivelRisco.MODERADO, analise.nivel());
        assertEquals(
                Set.of(TipoAlertaFraude.VALOR_ELEVADO),
                analise.regrasAcionadas()
        );
    }

    @Test
    void deveClassificarFrequenciaElevadaComoRiscoModerado() {

        Long origemId = jdbcTemplate.queryForObject(
                """
                INSERT INTO conta_entity (titular, saldo)
                VALUES ('Carlos Teste', 2000.00)
                RETURNING id
                """,
                Long.class
        );

        Long destinoId = jdbcTemplate.queryForObject(
                """
                INSERT INTO conta_entity (titular, saldo)
                VALUES ('Lucas Teste', 500.00)
                RETURNING id
                """,
                Long.class
        );

        // Primeira transferência
        transferenciaService.transferir(
                origemId, destinoId,
                new BigDecimal("100.00"),
                UUID.randomUUID().toString()
        );

        // Segunda transferência
        transferenciaService.transferir(
                origemId, destinoId,
                new BigDecimal("100.00"),
                UUID.randomUUID().toString()
        );

        // Terceira transferência
        ResultadoTransferencia resultado = transferenciaService.transferir(
                origemId, destinoId,
                new BigDecimal("100.00"),
                UUID.randomUUID().toString()
        );

        ResultadoAnaliseRisco analise =
                antifraudeService.avaliarRisco(resultado.transferencia());

        assertEquals(45, analise.pontuacao());
        assertEquals(NivelRisco.MODERADO, analise.nivel());

        assertEquals(
                Set.of(TipoAlertaFraude.FREQUENCIA_ELEVADA),
                analise.regrasAcionadas()
        );
    }

    @Test
    void deveClassificarDuasRegrasComoRiscoAlto() {

        Long origemId = jdbcTemplate.queryForObject(
                """
                INSERT INTO conta_entity (titular, saldo)
                VALUES ('Pedro Teste', 10000.00)
                RETURNING id
                """,
                Long.class
        );

        Long destinoId = jdbcTemplate.queryForObject(
                """
                INSERT INTO conta_entity (titular, saldo)
                VALUES ('João Teste', 500.00)
                RETURNING id
                """,
                Long.class
        );

        // Primeira e segunda transferências
        for (int i = 0; i < 2; i++) {
            transferenciaService.transferir(
                    origemId,
                    destinoId,
                    new BigDecimal("100.00"),
                    UUID.randomUUID().toString()
            );
        }

        // Terceira transferência: valor elevado + frequência elevada
        ResultadoTransferencia resultado = transferenciaService.transferir(
                origemId,
                destinoId,
                new BigDecimal("6000.00"),
                UUID.randomUUID().toString()
        );

        ResultadoAnaliseRisco analise =
                antifraudeService.avaliarRisco(resultado.transferencia());

        assertEquals(80, analise.pontuacao());
        assertEquals(NivelRisco.ALTO, analise.nivel());

        assertEquals(
                Set.of(
                        TipoAlertaFraude.VALOR_ELEVADO,
                        TipoAlertaFraude.FREQUENCIA_ELEVADA
                ),
                analise.regrasAcionadas()
        );
    }
}
