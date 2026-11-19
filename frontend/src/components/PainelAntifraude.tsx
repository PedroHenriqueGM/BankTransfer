import { useEffect, useState } from 'react'
import {
    buscarSinalizadas,
    buscarAlertas,
    type Transferencia,
    type AlertaFraude
} from '../services/transferenciaService'

type FiltroRisco = 'TODOS' | 'MODERADO' | 'ALTO'

function PainelAntifraude() {
    const [transferencias, setTransferencias] = useState<Transferencia[]>([])
    const [filtro, setFiltro] = useState<FiltroRisco>('TODOS')
    const [carregando, setCarregando] = useState(true)
    const [erro, setErro] = useState('')
    const [transferenciaSelecionada, setTransferenciaSelecionada] =
        useState<Transferencia | null>(null)

    const [alertas, setAlertas] = useState<AlertaFraude[]>([])
    const [carregandoAlertas, setCarregandoAlertas] = useState(false)
    const [erroAlertas, setErroAlertas] = useState('')

    useEffect(() => {
        let ativo = true

        async function carregarTransferencias() {
            try {
                const dados = await buscarSinalizadas()

                if (ativo) {
                    setTransferencias(dados)
                }
            } catch {
                if (ativo) {
                    setErro('Não foi possível carregar o painel antifraude.')
                }
            } finally {
                if (ativo) {
                    setCarregando(false)
                }
            }
        }

        carregarTransferencias()

        return () => {
            ativo = false
        }
    }, [])

    const quantidadeModerado = transferencias.filter(
        t => t.nivelRisco === 'MODERADO'
    ).length

    const quantidadeAlto = transferencias.filter(
        t => t.nivelRisco === 'ALTO'
    ).length

    const transferenciasFiltradas = transferencias.filter(
        t => filtro === 'TODOS' || t.nivelRisco === filtro
    )


    async function selecionarTransferencia(transferencia: Transferencia) {
        setTransferenciaSelecionada(transferencia)
        setAlertas([])
        setErroAlertas('')
        setCarregandoAlertas(true)

        try {
            const dados = await buscarAlertas(transferencia.id)
            setAlertas(dados)
        } catch {
            setErroAlertas('Não foi possível carregar os alertas.')
        } finally {
            setCarregandoAlertas(false)
        }
    }


    return (
        <section>
            <h2>Monitoramento Antifraude</h2>
            <p>Transferências sinalizadas pelas regras de risco.</p>

            {carregando && <p>Carregando operações...</p>}

            {erro && <p className="mensagem-erro">{erro}</p>}

            {!carregando && !erro && (
                <>
                    <div className="resumo-antifraude">
                        <div className="indicador-risco">
                            <h3>Sinalizadas</h3>
                            <strong>{transferencias.length}</strong>
                        </div>

                        <div className="indicador-risco">
                            <h3>Risco moderado</h3>
                            <strong>{quantidadeModerado}</strong>
                        </div>

                        <div className="indicador-risco">
                            <h3>Risco alto</h3>
                            <strong>{quantidadeAlto}</strong>
                        </div>
                    </div>

                    <div className="filtro-antifraude">
                        <label htmlFor="filtroRisco">Filtrar por risco:</label>

                        <select
                            id="filtroRisco"
                            value={filtro}
                            onChange={e =>
                                setFiltro(e.target.value as FiltroRisco)
                            }
                        >
                            <option value="TODOS">Todos</option>
                            <option value="MODERADO">Moderado</option>
                            <option value="ALTO">Alto</option>
                        </select>
                    </div>

                    {transferenciasFiltradas.length === 0 ? (
                        <p className="mensagem-vazia">
                            Nenhuma transferência encontrada para este filtro.
                        </p>
                    ) : (
                        <div className="tabela-container">
                            <table className="tabela-historico">
                                <thead>
                                <tr>
                                    <th>ID</th>
                                    <th>ID Origem</th>
                                    <th>ID Destino</th>
                                    <th>Valor</th>
                                    <th>Data</th>
                                    <th>Pontuação</th>
                                    <th>Risco</th>
                                    <th>Status</th>
                                    <th>Detalhes</th>
                                </tr>
                                </thead>

                                <tbody>
                                {transferenciasFiltradas.map(t => (
                                    <tr key={t.id}>
                                        <td>{t.id}</td>
                                        <td>{t.origemId}</td>
                                        <td>{t.destinoId}</td>

                                        <td>
                                            {t.valor.toLocaleString('pt-BR', {
                                                style: 'currency',
                                                currency: 'BRL'
                                            })}
                                        </td>

                                        <td>
                                            {new Date(t.criadaEm).toLocaleString('pt-BR')}
                                        </td>

                                        <td>{t.pontuacaoRisco}</td>
                                        <td>{t.nivelRisco}</td>

                                        <td>
                                            <span className={
                                                      t.status === 'CONCLUIDA'
                                                          ? 'status-concluida'
                                                          : 'status-recusada'
                                                  }>
                                              {t.status}
                                            </span>
                                        </td>
                                        <td>
                                            <button
                                                type="button"
                                                className="botao-detalhes"
                                                onClick={() => selecionarTransferencia(t)}>
                                                Ver alertas
                                            </button>
                                        </td>
                                    </tr>
                                ))}
                                </tbody>
                            </table>
                        </div>
                    )}

                    {transferenciaSelecionada && (
                        <div
                            className="modal-overlay"
                            onClick={() => setTransferenciaSelecionada(null)}
                        >
                            <div
                                className="modal-antifraude"
                                role="dialog"
                                aria-modal="true"
                                aria-labelledby="titulo-alertas"
                                onClick={e => e.stopPropagation()}
                            >
                                <div className="modal-cabecalho">
                                    <h3 id="titulo-alertas">
                                        Alertas da transferência #{transferenciaSelecionada.id}
                                    </h3>

                                    <button
                                        type="button"
                                        className="modal-fechar"
                                        aria-label="Fechar alertas"
                                        onClick={() => setTransferenciaSelecionada(null)}
                                    >
                                        ×
                                    </button>
                                </div>

                                {carregandoAlertas && <p>Carregando alertas...</p>}

                                {erroAlertas && (
                                    <p className="mensagem-erro">{erroAlertas}</p>
                                )}

                                {!carregandoAlertas && !erroAlertas && (
                                    alertas.length === 0 ? (
                                        <p>Nenhum alerta registrado.</p>
                                    ) : (
                                        <ul className="lista-alertas">
                                            {alertas.map(alerta => (
                                                <li key={alerta.id}>
                                                    {alerta.tipo === 'VALOR_ELEVADO'
                                                        ? 'Valor elevado'
                                                        : alerta.tipo === 'FREQUENCIA_ELEVADA'
                                                            ? 'Frequência elevada'
                                                            : alerta.tipo}
                                                </li>
                                            ))}
                                        </ul>
                                    )
                                )}
                            </div>
                        </div>
                    )}

                </>
            )}
        </section>
    )
}

export default PainelAntifraude
