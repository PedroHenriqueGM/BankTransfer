package com.pedro.banktransfer.service;

import com.pedro.banktransfer.entity.ContaEntity;
import com.pedro.banktransfer.exception.ContaNaoEncontradaException;
import com.pedro.banktransfer.repository.ContaRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class ContaService {

    private final ContaRepository contaRepository;

    public ContaService(ContaRepository contaRepository) {
        this.contaRepository = contaRepository;
    }

    public ContaEntity criarConta(String titular, BigDecimal saldoInicial) {
        BigDecimal saldo = saldoInicial == null
                ? BigDecimal.ZERO
                : saldoInicial;

        if (saldo.signum() < 0 ||
                saldo.scale() > 2 ||
                saldo.precision() - saldo.scale() > 17) {
            throw new IllegalArgumentException(
                    "Saldo inicial inválido. Informe um valor não negativo com no máximo duas casas decimais."
            );
        }

        ContaEntity conta = new ContaEntity(titular, saldo);
        return contaRepository.save(conta);
    }


    public ContaEntity buscarPorId(Long id) {
        return contaRepository.findById(id)
                .orElseThrow(() ->
                        new ContaNaoEncontradaException(id));
    }
}
