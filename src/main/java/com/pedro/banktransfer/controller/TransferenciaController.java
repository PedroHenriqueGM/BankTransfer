package com.pedro.banktransfer.controller;

import com.pedro.banktransfer.dto.CriarTransferenciaDTO;
import com.pedro.banktransfer.dto.TransferenciaResponseDTO;
import com.pedro.banktransfer.entity.ContaEntity;
import com.pedro.banktransfer.entity.TransferenciaEntity;
import com.pedro.banktransfer.service.ResultadoTransferencia;
import com.pedro.banktransfer.service.TransferenciaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/transferencias")
public class TransferenciaController {

    private final TransferenciaService transferenciaService;

    public TransferenciaController(TransferenciaService transferenciaService) {
        this.transferenciaService = transferenciaService;
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
        // 2. Converter a entidade em DTO
        TransferenciaResponseDTO resposta = new TransferenciaResponseDTO(
                salva.getId(),
                salva.getContaOrigem().getId(),
                salva.getContaDestino().getId(),
                salva.getValor(),
                salva.getStatus(),
                salva.getCriadaEm(),
                salva.getPontuacaoRisco(),
                salva.getNivelRisco()
        );
        // 3. Retornar HTTP 201 com o DTO
        HttpStatus status = resultado.criada()
                ? HttpStatus.CREATED
                : HttpStatus.OK;

        return ResponseEntity.status(status).body(resposta);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TransferenciaResponseDTO> buscarPorId(@PathVariable Long id) {
        TransferenciaEntity transferencia = transferenciaService.buscarPorId(id);
        // Converter a entidade em TransferenciaResponseDTO
        TransferenciaResponseDTO resposta = new TransferenciaResponseDTO(
                transferencia.getId(),
                transferencia.getContaOrigem().getId(),
                transferencia.getContaDestino().getId(),
                transferencia.getValor(),
                transferencia.getStatus(),
                transferencia.getCriadaEm(),
                transferencia.getPontuacaoRisco(),
                transferencia.getNivelRisco()
        );
        // Retornar ResponseEntity.ok(resposta)
        return ResponseEntity.ok(resposta);
    }
}