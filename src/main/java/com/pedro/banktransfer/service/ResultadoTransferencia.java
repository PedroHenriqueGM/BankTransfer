package com.pedro.banktransfer.service;

import com.pedro.banktransfer.entity.TransferenciaEntity;

public record ResultadoTransferencia(TransferenciaEntity transferencia, boolean criada) {
}
