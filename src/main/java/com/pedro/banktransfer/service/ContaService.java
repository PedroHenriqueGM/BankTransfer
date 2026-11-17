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

    public ContaEntity criarConta(String titular) {
        ContaEntity conta = new ContaEntity(titular);
        return contaRepository.save(conta);

    }

    public ContaEntity buscarPorId(Long id) {
        return contaRepository.findById(id)
                .orElseThrow(() ->
                        new ContaNaoEncontradaException(id));
    }
}
