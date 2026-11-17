package com.pedro.banktransfer.service;

import com.pedro.banktransfer.dto.TransferenciaResponseDTO;
import com.pedro.banktransfer.entity.ContaEntity;
import com.pedro.banktransfer.entity.TransferenciaEntity;
import com.pedro.banktransfer.exception.ChaveIdempotenciaConflitanteException;
import com.pedro.banktransfer.exception.ContaNaoEncontradaException;
import com.pedro.banktransfer.exception.TransferenciaNaoEncontradaException;
import com.pedro.banktransfer.repository.AlertaFraudeRepository;
import com.pedro.banktransfer.repository.ContaRepository;
import com.pedro.banktransfer.repository.TransferenciaRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;

@Service
public class TransferenciaService {
    private final ContaRepository contaRepository;
    private final TransferenciaRepository transferenciaRepository;
    private final JdbcTemplate jdbcTemplate;
    private final AntifraudeService antiFraudeService;

    public TransferenciaService(
            ContaRepository contaRepository,
            TransferenciaRepository transferenciaRepository,
            JdbcTemplate jdbcTemplate,
            AntifraudeService antifraudeService) {

            this.contaRepository = contaRepository;
            this.transferenciaRepository = transferenciaRepository;
            this.jdbcTemplate = jdbcTemplate;
            this.antiFraudeService = antifraudeService;
    }

    @Transactional
    public ResultadoTransferencia transferir(Long origemId, Long destinoId, BigDecimal valor, String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException(
                    "Chave de idempotência obrigatória"
            );
        }
        idempotencyKey = idempotencyKey.trim();

        if (idempotencyKey.length() > 100) {
            throw new IllegalArgumentException(
                    "Chave de idempotência muito longa"
            );
        }
        // Bloqueia o processamento simultâneo da mesma chave
        jdbcTemplate.query(
                "SELECT pg_advisory_xact_lock(hashtextextended(?, 0))",
                rs -> {},
                idempotencyKey
        );
        Optional<TransferenciaEntity> existente =
                transferenciaRepository.findByIdempotencyKey(idempotencyKey);
        if (existente.isPresent()) {
            TransferenciaEntity transferencia = existente.get();

            if (!Objects.equals(transferencia.getContaOrigem().getId(), origemId)
                    || !Objects.equals(transferencia.getContaDestino().getId(), destinoId)
                    || transferencia.getValor().compareTo(valor) != 0) {

                throw new ChaveIdempotenciaConflitanteException();
            }

            return new ResultadoTransferencia(transferencia, false);
        }
        if (origemId == null || destinoId == null) {
            throw new IllegalArgumentException("As contas devem ser informadas");
        }
        if (Objects.equals(origemId, destinoId)){
            throw new IllegalArgumentException("Não é possível transferir para a mesma conta");
        }
        if (valor == null || valor.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Valor deve ser positivo");
        }
        Long primeiroId = Math.min(origemId, destinoId);
        Long segundoId = Math.max(origemId, destinoId);

        ContaEntity primeiraConta = contaRepository.buscarComBloqueio(primeiroId)
                .orElseThrow(() -> new ContaNaoEncontradaException(primeiroId));

        ContaEntity segundaConta = contaRepository.buscarComBloqueio(segundoId)
                .orElseThrow(() -> new ContaNaoEncontradaException(segundoId));
        ContaEntity origem = Objects.equals(origemId, primeiroId)
                ? primeiraConta
                : segundaConta;

        ContaEntity destino = Objects.equals(destinoId, primeiroId)
                ? primeiraConta
                : segundaConta;
        origem.debitar(valor);
        destino.creditar(valor);
        TransferenciaEntity transferencia = new TransferenciaEntity(origem, destino, valor, idempotencyKey);
        TransferenciaEntity salva =
                transferenciaRepository.saveAndFlush(transferencia);

        antiFraudeService.analisarTransferencia(salva);
        return new ResultadoTransferencia(salva, true);
    }

    public TransferenciaEntity buscarPorId(Long id) {
        return transferenciaRepository.findById(id)
                .orElseThrow(() ->
                        new TransferenciaNaoEncontradaException(id));
    }

    public List<TransferenciaResponseDTO> listarPorConta(Long contaId) {
        if (!contaRepository.existsById(contaId)) {
            throw new ContaNaoEncontradaException(contaId);
        }

        return transferenciaRepository
                .findByContaOrigemIdOrContaDestinoId(contaId, contaId)
                .stream()
                .map(t -> new TransferenciaResponseDTO(
                        t.getId(),
                        t.getContaOrigem().getId(),
                        t.getContaDestino().getId(),
                        t.getValor(),
                        t.getStatus(),
                        t.getCriadaEm(),
                        t.getPontuacaoRisco(),
                        t.getNivelRisco()
                ))
                .toList();
    }


}
