package com.pedro.banktransfer.repository;

import com.pedro.banktransfer.entity.ContaEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ContaRepository extends JpaRepository<ContaEntity, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM ContaEntity c WHERE c.id = :id")
    Optional<ContaEntity> buscarComBloqueio(@Param("id") Long id);
}
