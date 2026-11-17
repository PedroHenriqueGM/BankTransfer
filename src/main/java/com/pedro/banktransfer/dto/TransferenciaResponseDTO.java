package com.pedro.banktransfer.dto;

import com.pedro.banktransfer.enums.NivelRisco;
import com.pedro.banktransfer.enums.StatusTransferencia;
import java.math.BigDecimal;
import java.time.Instant;

public record TransferenciaResponseDTO(
        Long id,
        Long origemId,
        Long destinoId,
        BigDecimal valor,
        StatusTransferencia status,
        Instant criadaEm,
        Integer pontuacaoRisco,
        NivelRisco nivelRisco
) {}