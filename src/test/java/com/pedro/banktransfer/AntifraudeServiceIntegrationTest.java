package com.pedro.banktransfer;

import com.pedro.banktransfer.enums.NivelRisco;
import com.pedro.banktransfer.enums.StatusTransferencia;
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
import java.util.List;
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

        assertEquals(
                StatusTransferencia.CONCLUIDA,
                resultado.transferencia().getStatus()
        );

        assertEquals(0, resultado.transferencia().getPontuacaoRisco());
        assertEquals(NivelRisco.BAIXO, resultado.transferencia().getNivelRisco());
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

        assertEquals(
                StatusTransferencia.CONCLUIDA,
                resultado.transferencia().getStatus()
        );

        assertEquals(35, resultado.transferencia().getPontuacaoRisco());
        assertEquals(NivelRisco.MODERADO, resultado.transferencia().getNivelRisco());
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

        assertEquals(
                StatusTransferencia.CONCLUIDA,
                resultado.transferencia().getStatus()
        );

        assertEquals(45, resultado.transferencia().getPontuacaoRisco());
        assertEquals(NivelRisco.MODERADO, resultado.transferencia().getNivelRisco());
    }


    @Test
    void deveRecusarTransferenciaDeRiscoAltoSemAlterarSaldos() {

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

        // Duas transferências concluídas
        for (int i = 0; i < 2; i++) {
            transferenciaService.transferir(
                    origemId,
                    destinoId,
                    new BigDecimal("100.00"),
                    UUID.randomUUID().toString()
            );
        }

        // Consulta os saldos antes da tentativa de risco alto
        BigDecimal saldoOrigemAntes = jdbcTemplate.queryForObject(
                "SELECT saldo FROM conta_entity WHERE id = ?",
                BigDecimal.class,
                origemId
        );

        BigDecimal saldoDestinoAntes = jdbcTemplate.queryForObject(
                "SELECT saldo FROM conta_entity WHERE id = ?",
                BigDecimal.class,
                destinoId
        );

        // Terceira transferência: valor elevado + frequência elevada
        ResultadoTransferencia resultado = transferenciaService.transferir(
                origemId,
                destinoId,
                new BigDecimal("6000.00"),
                UUID.randomUUID().toString()
        );

        // Verifica se a transferência foi recusada
        assertEquals(
                StatusTransferencia.RECUSADA,
                resultado.transferencia().getStatus()
        );

        assertEquals(
                80,
                resultado.transferencia().getPontuacaoRisco()
        );

        assertEquals(
                NivelRisco.ALTO,
                resultado.transferencia().getNivelRisco()
        );

        // Consulta os saldos após a tentativa recusada
        BigDecimal saldoOrigemDepois = jdbcTemplate.queryForObject(
                "SELECT saldo FROM conta_entity WHERE id = ?",
                BigDecimal.class,
                origemId
        );

        BigDecimal saldoDestinoDepois = jdbcTemplate.queryForObject(
                "SELECT saldo FROM conta_entity WHERE id = ?",
                BigDecimal.class,
                destinoId
        );

        // Nenhuma movimentação financeira deve ocorrer
        assertEquals(
                0,
                saldoOrigemAntes.compareTo(saldoOrigemDepois)
        );

        assertEquals(
                0,
                saldoDestinoAntes.compareTo(saldoDestinoDepois)
        );

        // Verifica se foram registrados exatamente dois alertas
        Integer quantidadeAlertas = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM alerta_fraude_entity
                WHERE transferencia_id = ?
                """,
                Integer.class,
                resultado.transferencia().getId()
        );

        assertEquals(2, quantidadeAlertas);

        // Verifica os tipos dos alertas
        List<String> tiposAlertas = jdbcTemplate.queryForList(
                """
                SELECT tipo
                FROM alerta_fraude_entity
                WHERE transferencia_id = ?
                """,
                String.class,
                resultado.transferencia().getId()
        );

        assertEquals(
                Set.of("VALOR_ELEVADO", "FREQUENCIA_ELEVADA"),
                Set.copyOf(tiposAlertas)
        );
    }


    @Test
    void deveRespeitarIdempotenciaDeTransferenciaRecusada() {

        Long origemId = jdbcTemplate.queryForObject(
                """
                INSERT INTO conta_entity (titular, saldo)
                VALUES ('Origem Idempotencia', 10000.00)
                RETURNING id
                """,
                Long.class
        );

        Long destinoId = jdbcTemplate.queryForObject(
                """
                INSERT INTO conta_entity (titular, saldo)
                VALUES ('Destino Idempotencia', 500.00)
                RETURNING id
                """,
                Long.class
        );

        // Duas transferências concluídas
        for (int i = 0; i < 2; i++) {
            transferenciaService.transferir(
                    origemId,
                    destinoId,
                    new BigDecimal("100.00"),
                    UUID.randomUUID().toString()
            );
        }

        String chave = UUID.randomUUID().toString();

        // Primeira tentativa: recusada por risco alto
        ResultadoTransferencia primeira = transferenciaService.transferir(
                origemId,
                destinoId,
                new BigDecimal("6000.00"),
                chave
        );

        // Repete exatamente a mesma solicitação
        ResultadoTransferencia repetida = transferenciaService.transferir(
                origemId,
                destinoId,
                new BigDecimal("6000.00"),
                chave
        );

        assertEquals(
                StatusTransferencia.RECUSADA,
                primeira.transferencia().getStatus()
        );

        // A segunda solicitação deve retornar o mesmo registro
        assertEquals(
                primeira.transferencia().getId(),
                repetida.transferencia().getId()
        );

        assertEquals(false, repetida.criada());

        // Nenhuma transferência adicional com essa chave
        Integer quantidadeTransferencias = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM transferencia_entity
                WHERE idempotency_key = ?
                """,
                Integer.class,
                chave
        );

        assertEquals(1, quantidadeTransferencias);

        // Os dois alertas não podem ser duplicados
        Integer quantidadeAlertas = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM alerta_fraude_entity
                WHERE transferencia_id = ?
                """,
                Integer.class,
                primeira.transferencia().getId()
        );

        assertEquals(2, quantidadeAlertas);
    }


    @Test
    void naoDeveContabilizarTransferenciasRecusadasNaFrequencia() {

        Long origemId = jdbcTemplate.queryForObject(
                """
                INSERT INTO conta_entity (titular, saldo)
                VALUES ('Origem Frequencia', 10000.00)
                RETURNING id
                """,
                Long.class
        );

        Long destinoId = jdbcTemplate.queryForObject(
                """
                INSERT INTO conta_entity (titular, saldo)
                VALUES ('Destino Frequencia', 500.00)
                RETURNING id
                """,
                Long.class
        );

        // Duas transferências concluídas
        for (int i = 0; i < 2; i++) {
            transferenciaService.transferir(
                    origemId,
                    destinoId,
                    new BigDecimal("100.00"),
                    UUID.randomUUID().toString()
            );
        }

        // Terceira solicitação: recusada por risco ALTO
        ResultadoTransferencia recusada =
                transferenciaService.transferir(
                        origemId,
                        destinoId,
                        new BigDecimal("6000.00"),
                        UUID.randomUUID().toString()
                );

        assertEquals(
                StatusTransferencia.RECUSADA,
                recusada.transferencia().getStatus()
        );

        // Quarta solicitação: valor pequeno
        Long concluidasAntesDaQuarta = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM transferencia_entity
                WHERE conta_origem_id = ?
                  AND status = 'CONCLUIDA'
                """,
                Long.class,
                origemId
        );

        assertEquals(2L, concluidasAntesDaQuarta);
        ResultadoTransferencia quarta =
                transferenciaService.transferir(
                        origemId,
                        destinoId,
                        new BigDecimal("100.00"),
                        UUID.randomUUID().toString()
                );

        assertEquals(
                StatusTransferencia.CONCLUIDA,
                quarta.transferencia().getStatus()
        );

        assertEquals(
                45,
                quarta.transferencia().getPontuacaoRisco()
        );

        assertEquals(
                NivelRisco.MODERADO,
                quarta.transferencia().getNivelRisco()
        );

        // Apenas três transferências foram efetivamente concluídas
        Long quantidadeConcluidas = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM transferencia_entity
                WHERE conta_origem_id = ?
                  AND status = 'CONCLUIDA'
                """,
                Long.class,
                origemId
        );

        assertEquals(3L, quantidadeConcluidas);
    }



}
