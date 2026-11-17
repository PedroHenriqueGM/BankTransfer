package com.pedro.banktransfer.service;

import com.pedro.banktransfer.entity.AlertaFraudeEntity;
import com.pedro.banktransfer.entity.TransferenciaEntity;
import com.pedro.banktransfer.enums.NivelRisco;
import com.pedro.banktransfer.enums.TipoAlertaFraude;
import com.pedro.banktransfer.repository.AlertaFraudeRepository;
import com.pedro.banktransfer.repository.TransferenciaRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.Set;

@Service
public class AntifraudeService {

    private final AlertaFraudeRepository alertaFraudeRepository;
    private final TransferenciaRepository transferenciaRepository;

    public AntifraudeService(AlertaFraudeRepository alertaFraudeRepository, TransferenciaRepository transferenciaRepository) {
        this.alertaFraudeRepository = alertaFraudeRepository;
        this.transferenciaRepository = transferenciaRepository;
    }

    public void analisarTransferencia(TransferenciaEntity transferencia) {

        ResultadoAnaliseRisco resultado = avaliarRisco(transferencia);

        transferencia.registrarRisco(resultado);

        for (TipoAlertaFraude tipo : resultado.regrasAcionadas()) {

            AlertaFraudeEntity alerta = new AlertaFraudeEntity(
                    transferencia,
                    tipo,
                    "Regra de risco acionada: " + tipo
            );

            alertaFraudeRepository.save(alerta);
        }
    }

    public ResultadoAnaliseRisco avaliarRisco(
            TransferenciaEntity transferencia) {

        int pontuacao = 0;
        Set<TipoAlertaFraude> regras = new HashSet<>();

        // Regra 1: Valor elevado
        if (transferencia.getValor()
                .compareTo(new BigDecimal("5000.00")) > 0) {

            pontuacao += 35;
            regras.add(TipoAlertaFraude.VALOR_ELEVADO);
        }

        // Regra 2: Frequência elevada
        Instant cincoMinutosAtras = transferencia.getCriadaEm()
                .minus(5, ChronoUnit.MINUTES);

        long quantidade = transferenciaRepository
                .countByContaOrigemIdAndCriadaEmAfter(
                        transferencia.getContaOrigem().getId(),
                        cincoMinutosAtras
                );

        if (quantidade >= 3) {
            pontuacao += 45;
            regras.add(TipoAlertaFraude.FREQUENCIA_ELEVADA);
        }

        // Classificação
        NivelRisco nivel;

        if (pontuacao >= 70) {
            nivel = NivelRisco.ALTO;
        } else if (pontuacao >= 35) {
            nivel = NivelRisco.MODERADO;
        } else {
            nivel = NivelRisco.BAIXO;
        }

        return new ResultadoAnaliseRisco(
                pontuacao,
                nivel,
                Set.copyOf(regras)
        );
    }
}
