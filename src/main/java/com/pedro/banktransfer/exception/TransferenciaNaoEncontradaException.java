package com.pedro.banktransfer.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class TransferenciaNaoEncontradaException extends RuntimeException {

    public TransferenciaNaoEncontradaException(Long id) {
        super("Transferência não encontrada: " + id);
    }
}
