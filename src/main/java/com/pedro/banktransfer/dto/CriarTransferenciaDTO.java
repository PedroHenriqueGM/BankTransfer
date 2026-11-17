package com.pedro.banktransfer.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CriarTransferenciaDTO(
        @NotNull Long origemId,
        @NotNull Long destinoId,

        @NotNull
        @DecimalMin("0.01")
        BigDecimal valor
) {

}