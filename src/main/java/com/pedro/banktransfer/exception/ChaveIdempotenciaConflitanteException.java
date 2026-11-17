package com.pedro.banktransfer.exception;

public class ChaveIdempotenciaConflitanteException extends RuntimeException {
    public ChaveIdempotenciaConflitanteException() {
        super("Chave de idempotência já utilizada com outros dados");
    }
}
