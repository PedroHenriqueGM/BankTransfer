package com.pedro.banktransfer.dto;

import com.pedro.banktransfer.enums.TipoAlertaFraude;

import java.time.Instant;

public record AlertaFraudeResponseDTO(
        Long id,
        Long transferenciaId,
        TipoAlertaFraude tipo,
        String descricao,
        Instant criadoEm
) {
}