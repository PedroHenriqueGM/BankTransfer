package com.pedro.banktransfer.entity;

import com.pedro.banktransfer.enums.NivelRisco;
import com.pedro.banktransfer.enums.StatusTransferencia;
import com.pedro.banktransfer.service.ResultadoAnaliseRisco;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
public class TransferenciaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "conta_origem_id", nullable = false)
    private ContaEntity contaOrigem;

    @ManyToOne
    @JoinColumn(name = "conta_destino_id", nullable = false)
    private ContaEntity contaDestino;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal valor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusTransferencia status;

    @Column(nullable = false)
    private Instant criadaEm;

    @Column(name = "idempotency_key", unique = true, length = 100)
    private String idempotencyKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "nivel_risco")
    private NivelRisco nivelRisco;

    @Column(name = "pontuacao_risco")
    private Integer pontuacaoRisco;

    public TransferenciaEntity() {

    }
    public TransferenciaEntity(
            ContaEntity contaOrigem,
            ContaEntity contaDestino,
            BigDecimal valor,
            String idempotencyKey) {

        this.contaOrigem = contaOrigem;
        this.contaDestino = contaDestino;
        this.valor = valor;
        this.status = StatusTransferencia.CONCLUIDA;
        this.criadaEm = Instant.now();
        this.idempotencyKey =idempotencyKey;
    }

    public void registrarRisco(ResultadoAnaliseRisco resultado) {
        this.nivelRisco = resultado.nivel();
        this.pontuacaoRisco = resultado.pontuacao();
    }

    public Long getId() {
        return id;
    }

    public ContaEntity getContaOrigem() {
        return contaOrigem;
    }

    public ContaEntity getContaDestino() {
        return contaDestino;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public StatusTransferencia getStatus() {
        return status;
    }

    public Instant getCriadaEm() {
        return criadaEm;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public NivelRisco getNivelRisco() {
        return nivelRisco;
    }

    public Integer getPontuacaoRisco() {
        return pontuacaoRisco;
    }
}