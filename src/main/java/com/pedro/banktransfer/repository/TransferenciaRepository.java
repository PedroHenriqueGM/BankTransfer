package com.pedro.banktransfer.repository;

import com.pedro.banktransfer.entity.ContaEntity;
import com.pedro.banktransfer.entity.TransferenciaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface TransferenciaRepository extends JpaRepository<TransferenciaEntity, Long> {

    Optional<TransferenciaEntity> findByIdempotencyKey(String idempotencyKey);

    List<TransferenciaEntity> findByContaOrigemIdOrContaDestinoId(
            Long origemId,
            Long destinoId
    );

    long countByContaOrigemIdAndCriadaEmAfter(
            Long contaId,
            Instant instante
    );
}
