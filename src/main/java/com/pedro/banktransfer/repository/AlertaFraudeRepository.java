package com.pedro.banktransfer.repository;

import com.pedro.banktransfer.entity.AlertaFraudeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AlertaFraudeRepository extends JpaRepository<AlertaFraudeEntity, Long> {
    List<AlertaFraudeEntity> findByTransferenciaId(Long transferenciaId);
}
