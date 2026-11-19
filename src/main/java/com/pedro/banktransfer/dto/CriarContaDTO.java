package com.pedro.banktransfer.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;

public record CriarContaDTO(
        @NotBlank String titular,
        @DecimalMin("0.00")
        @Digits(integer = 17, fraction = 2)
        BigDecimal saldoInicial) {

}
