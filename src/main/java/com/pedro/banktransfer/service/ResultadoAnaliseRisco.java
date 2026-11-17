package com.pedro.banktransfer.service;

import com.pedro.banktransfer.enums.NivelRisco;
import com.pedro.banktransfer.enums.TipoAlertaFraude;

import java.util.Set;

public record ResultadoAnaliseRisco(
        int pontuacao,
        NivelRisco nivel,
        Set<TipoAlertaFraude> regrasAcionadas
) {
}
