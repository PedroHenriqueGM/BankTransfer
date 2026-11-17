package com.pedro.banktransfer.dto;

import jakarta.validation.constraints.NotBlank;

public record CriarContaDTO(@NotBlank String titular) {

}
