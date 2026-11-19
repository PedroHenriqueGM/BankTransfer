package com.pedro.banktransfer.controller;

import com.pedro.banktransfer.dto.CriarContaDTO;
import com.pedro.banktransfer.dto.TransferenciaResponseDTO;
import com.pedro.banktransfer.entity.ContaEntity;
import com.pedro.banktransfer.service.ContaService;
import com.pedro.banktransfer.service.TransferenciaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/contas")
public class ContaController {
    private final ContaService contaService;
    private final TransferenciaService transferenciaService;

    public ContaController(ContaService contaService, TransferenciaService transferenciaService) {
        this.contaService = contaService;
        this.transferenciaService = transferenciaService;
    }

    @PostMapping
    public ResponseEntity<ContaEntity> criarConta(@Valid @RequestBody CriarContaDTO dto) {
        ContaEntity salva = contaService.criarConta(
                dto.titular(),
                dto.saldoInicial()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(salva);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ContaEntity> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(contaService.buscarPorId(id));
    }

    @GetMapping("/{id}/transferencias")
    public ResponseEntity<List<TransferenciaResponseDTO>> listarPorConta(@PathVariable Long id) {
        return ResponseEntity.ok(transferenciaService.listarPorConta(id));
    }

}
