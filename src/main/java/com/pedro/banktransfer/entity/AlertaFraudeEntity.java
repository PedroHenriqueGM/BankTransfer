package com.pedro.banktransfer.entity;

import com.pedro.banktransfer.enums.TipoAlertaFraude;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "alerta_fraude_entity")
public class AlertaFraudeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "transferencia_id", nullable = false)
    private TransferenciaEntity transferencia;

    @Enumerated(EnumType.STRING)
    private TipoAlertaFraude tipo;

    @Column(nullable = false)
    private String descricao;

    @Column(nullable = false)
    private Instant criadoEm;

    public AlertaFraudeEntity() {
    }

    public AlertaFraudeEntity(TransferenciaEntity transferencia, TipoAlertaFraude tipo, String descricao) {
        this.transferencia = transferencia;
        this.tipo = tipo;
        this.descricao = descricao;
        this.criadoEm = Instant.now();

    }

    public Long getId() {
        return id;
    }

    public TransferenciaEntity getTransferencia() {
        return transferencia;
    }

    public TipoAlertaFraude getTipo() {
        return tipo;
    }

    public String getDescricao() {
        return descricao;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }
}
