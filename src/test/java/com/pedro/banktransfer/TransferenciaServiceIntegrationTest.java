package com.pedro.banktransfer;

import com.pedro.banktransfer.service.ResultadoTransferencia;
import com.pedro.banktransfer.service.TransferenciaService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.math.BigDecimal;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Testcontainers
class TransferenciaServiceIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres =
            new PostgreSQLContainer("postgres:17-alpine");

    @Autowired
    private TransferenciaService transferenciaService;

    @Autowired
    private JdbcTemplate jdbcTemplate;


    // TESTE 1: Concorrência de saldo
    @Test
    void deveImpedirTransferenciasSimultaneasComSaldoInsuficiente()
            throws Exception {

        Long origemId = criarConta("Pedro", "600.00");
        Long destinoId = criarConta("João", "900.00");

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch prontas = new CountDownLatch(2);
        CountDownLatch inicio = new CountDownLatch(1);

        Callable<Boolean> tarefa = () -> {
            prontas.countDown();
            inicio.await();

            try {
                transferenciaService.transferir(
                        origemId,
                        destinoId,
                        new BigDecimal("400.00"),
                        java.util.UUID.randomUUID().toString()
                );
                return true;

            } catch (IllegalArgumentException ex) {
                if (!"Saldo insuficiente".equals(ex.getMessage())) {
                    throw ex;
                }
                return false;
            }
        };

        try {
            Future<Boolean> resultadoA = executor.submit(tarefa);
            Future<Boolean> resultadoB = executor.submit(tarefa);

            assertTrue(prontas.await(10, TimeUnit.SECONDS));
            inicio.countDown();

            boolean sucessoA = resultadoA.get(30, TimeUnit.SECONDS);
            boolean sucessoB = resultadoB.get(30, TimeUnit.SECONDS);

            assertNotEquals(sucessoA, sucessoB);

            assertSaldo(origemId, "200.00");
            assertSaldo(destinoId, "1300.00");

            assertEquals(1L, contarTransferencias(origemId, destinoId));

        } finally {
            inicio.countDown();
            executor.shutdownNow();
        }
    }

    // TESTE 2: Idempotência concorrente
    @Test
    void deveProcessarUmaUnicaTransferenciaComMesmaChave()
            throws Exception {

        Long origemId = criarConta("Maria", "600.00");
        Long destinoId = criarConta("Ana", "900.00");

        String chave = java.util.UUID.randomUUID().toString();

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch prontas = new CountDownLatch(2);
        CountDownLatch inicio = new CountDownLatch(1);

        Callable<ResultadoTransferencia> tarefa = () -> {
            prontas.countDown();
            inicio.await();

            return transferenciaService.transferir(
                    origemId,
                    destinoId,
                    new BigDecimal("100.00"),
                    chave
            );
        };

        try {
            Future<ResultadoTransferencia> futuroA =
                    executor.submit(tarefa);

            Future<ResultadoTransferencia> futuroB =
                    executor.submit(tarefa);

            assertTrue(prontas.await(10, TimeUnit.SECONDS));
            inicio.countDown();

            ResultadoTransferencia resultadoA =
                    futuroA.get(30, TimeUnit.SECONDS);

            ResultadoTransferencia resultadoB =
                    futuroB.get(30, TimeUnit.SECONDS);

            // Ambas devem retornar a mesma transferência
            assertEquals(
                    resultadoA.transferencia().getId(),
                    resultadoB.transferencia().getId()
            );

            // Uma criou; a outra reutilizou
            assertNotEquals(
                    resultadoA.criada(),
                    resultadoB.criada()
            );

            // Apenas R$ 100 devem ser movimentados
            assertSaldo(origemId, "500.00");
            assertSaldo(destinoId, "1000.00");

            // Apenas um registro com a chave
            Long quantidade = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM transferencia_entity " +
                            "WHERE idempotency_key = ?",
                    Long.class,
                    chave
            );

            assertEquals(1L, quantidade);

        } finally {
            inicio.countDown();
            executor.shutdownNow();
        }
    }

    // Métodos auxiliares

    private Long criarConta(String titular, String saldo) {
        return jdbcTemplate.queryForObject(
                "INSERT INTO conta_entity (titular, saldo) " +
                        "VALUES (?, ?) RETURNING id",
                Long.class,
                titular,
                new BigDecimal(saldo)
        );
    }

    private void assertSaldo(Long contaId, String esperado) {
        BigDecimal saldo = jdbcTemplate.queryForObject(
                "SELECT saldo FROM conta_entity WHERE id = ?",
                BigDecimal.class,
                contaId
        );

        assertNotNull(saldo);
        assertEquals(
                0,
                saldo.compareTo(new BigDecimal(esperado))
        );
    }

    private Long contarTransferencias(Long origemId, Long destinoId) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM transferencia_entity " +
                        "WHERE conta_origem_id = ? AND conta_destino_id = ?",
                Long.class,
                origemId,
                destinoId
        );
    }

}