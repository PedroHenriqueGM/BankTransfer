package com.pedro.banktransfer.controller;

import com.pedro.banktransfer.dto.AlertaFraudeResponseDTO;
import com.pedro.banktransfer.dto.CriarTransferenciaDTO;
import com.pedro.banktransfer.dto.TransferenciaResponseDTO;
import com.pedro.banktransfer.entity.ContaEntity;
import com.pedro.banktransfer.entity.TransferenciaEntity;
import com.pedro.banktransfer.service.AntifraudeService;
import com.pedro.banktransfer.service.ResultadoTransferencia;
import com.pedro.banktransfer.service.TransferenciaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/transferencias")
public class TransferenciaController {

    private final TransferenciaService transferenciaService;

    private final AntifraudeService antifraudeService;

    public TransferenciaController(TransferenciaService transferenciaService,
                                   AntifraudeService antifraudeService) {
        this.transferenciaService = transferenciaService;
        this.antifraudeService = antifraudeService;
    }

    @PostMapping
    public ResponseEntity<TransferenciaResponseDTO> transferir(
            @Valid @RequestBody CriarTransferenciaDTO dto,
            @RequestHeader("Idempotency-Key") String idempotencyKey) {
        // 1. Chamar transferenciaService.transferir(...)
        ResultadoTransferencia resultado = transferenciaService.transferir(
                dto.origemId(), dto.destinoId(), dto.valor(), idempotencyKey
        );
        TransferenciaEntity salva = resultado.transferencia();
        // 3. Retornar HTTP 201 com o DTO
        HttpStatus status = resultado.criada()
                ? HttpStatus.CREATED
                : HttpStatus.OK;

        return ResponseEntity.status(status).body(converterParaDTO(salva));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TransferenciaResponseDTO> buscarPorId(@PathVariable Long id) {
        TransferenciaEntity transferencia = transferenciaService.buscarPorId(id);
        return ResponseEntity.ok(converterParaDTO(transferencia));
    }

    @GetMapping("/sinalizadas")
    public List<TransferenciaResponseDTO> listarSinalizadas() {

        return transferenciaService.listarTransferenciasSinalizadas()
                .stream()
                .map(this::converterParaDTO)
                .toList();
    }


    @GetMapping("/{id}/alertas")
    public List<AlertaFraudeResponseDTO> buscarAlertas(
            @PathVariable Long id) {

        // Verifica se a transferência existe
        transferenciaService.buscarPorId(id);

        // Consulta os alertas associados
        return antifraudeService.buscarAlertasPorTransferencia(id)
                .stream()
                .map(alerta -> new AlertaFraudeResponseDTO(
                        alerta.getId(),
                        alerta.getTransferencia().getId(),
                        alerta.getTipo(),
                        alerta.getDescricao(),
                        alerta.getCriadoEm()
                ))
                .toList();
    }

    private TransferenciaResponseDTO converterParaDTO(
            TransferenciaEntity transferencia) {

        return new TransferenciaResponseDTO(
                transferencia.getId(),
                transferencia.getContaOrigem().getId(),
                transferencia.getContaDestino().getId(),
                transferencia.getValor(),
                transferencia.getStatus(),
                transferencia.getCriadaEm(),
                transferencia.getPontuacaoRisco(),
                transferencia.getNivelRisco()
        );
    }

}