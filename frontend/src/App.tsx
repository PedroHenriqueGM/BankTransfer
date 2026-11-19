import PainelAntifraude from './components/PainelAntifraude'
import { useRef, useState } from 'react'
import { buscarConta, criarConta, type Conta } from './services/contaService'
import './App.css'
import {
  criarTransferencia,
  buscarHistorico,
  type Transferencia,
} from './services/transferenciaService'

type Pagina = 'contas' | 'transferencias' | 'historico' | 'antifraude'

function App() {
  const [pagina, setPagina] = useState<Pagina>('contas')
  const [titular, setTitular] = useState('')
  const [saldoInicial, setSaldoInicial] = useState('')
  const [contaCriada, setContaCriada] = useState<Conta | null>(null)
  const [erro, setErro] = useState('')
  const [carregando, setCarregando] = useState(false)
  const [idConsulta, setIdConsulta] = useState('')
  const [contaConsultada, setContaConsultada] = useState<Conta | null>(null)
  const [erroConsulta, setErroConsulta] = useState('')
  const [consultando, setConsultando] = useState(false)
  const [origemId, setOrigemId] = useState('')
  const [destinoId, setDestinoId] = useState('')
  const [valor, setValor] = useState('')
  const [resultadoTransferencia, setResultadoTransferencia] =
      useState<Transferencia | null>(null)
  const [erroTransferencia, setErroTransferencia] = useState('')
  const [transferindo, setTransferindo] = useState(false)
  const tentativaPendente = useRef<{
    chave: string
    origem: number
    destino: number
    valor: number
  } | null>(null)
  const [idHistorico, setIdHistorico] = useState('')
  const [historico, setHistorico] = useState<Transferencia[]>([])
  const [erroHistorico, setErroHistorico] = useState('')
  const [consultandoHistorico, setConsultandoHistorico] = useState(false)
  const [historicoCarregado, setHistoricoCarregado] = useState(false)
  const [contaHistorico, setContaHistorico] = useState<number | null>(null)


  async function cadastrarConta(evento: React.SubmitEvent<HTMLFormElement>) {
    evento.preventDefault()

    if (!titular.trim()) {
      setErro('Informe o nome do titular.')
      return
    }

    const saldoInformado = saldoInicial.trim()

    if (
        saldoInformado !== '' &&
        (
            !/^\d+(\.\d{1,2})?$/.test(saldoInformado) ||
            saldoInformado.split('.')[0].length > 17
        )
    ) {
      setErro('Informe um saldo inicial válido, com no máximo duas casas decimais.')
      return
    }

    const saldoNumerico = saldoInformado === ''
        ? 0
        : Number(saldoInformado)

    if (!Number.isFinite(saldoNumerico)) {
      setErro('Informe um saldo inicial válido.')
      return
    }

    setCarregando(true)
    setErro('')
    setContaCriada(null)

    try {
      const conta = await criarConta(titular.trim(), saldoNumerico)

      setContaCriada(conta)
      setTitular('')
      setSaldoInicial('')
    } catch {
      setErro('Não foi possível cadastrar a conta.')
    } finally {
      setCarregando(false)
    }
  }



  async function consultarConta(evento: React.SubmitEvent<HTMLFormElement>) {
    evento.preventDefault()

    const id = Number(idConsulta)

    if (!Number.isSafeInteger(id) || id <= 0) {
      setErroConsulta('Informe um ID válido.')
      setContaConsultada(null)
      return
    }

    setConsultando(true)
    setErroConsulta('')
    setContaConsultada(null)

    try {
      const conta = await buscarConta(id)
      setContaConsultada(conta)
    } catch (erro) {
      setErroConsulta(
          erro instanceof Error ? erro.message : 'Erro ao consultar a conta.'
      )
    } finally {
      setConsultando(false)
    }
  }


  async function realizarTransferencia(
      evento: React.SubmitEvent<HTMLFormElement>
  ) {
    evento.preventDefault()

    const origem = Number(origemId)
    const destino = Number(destinoId)
    const valorInformado = valor.trim()
    const valorNumerico = Number(valorInformado)

    if (
        !Number.isSafeInteger(origem) ||
        !Number.isSafeInteger(destino) ||
        origem <= 0 ||
        destino <= 0 ||
        origem === destino
    ) {
      setErroTransferencia('Informe contas válidas e diferentes.')
      return
    }

    if (
        !/^\d+(\.\d{1,2})?$/.test(valorInformado) ||
        !Number.isFinite(valorNumerico) ||
        valorNumerico <= 0 ||
        valorInformado.split('.')[0].length > 17
    ) {
      setErroTransferencia(
          'Informe um valor positivo com até 17 dígitos inteiros e 2 casas decimais.'
      )
      return
    }

    setTransferindo(true)
    setErroTransferencia('')
    setResultadoTransferencia(null)

    try {
      const tentativa = tentativaPendente.current

      // Reutiliza a chave se os dados forem idênticos
      const mesmaOperacao =
          tentativa !== null &&
          tentativa.origem === origem &&
          tentativa.destino === destino &&
          tentativa.valor === valorNumerico

      const chave = mesmaOperacao
          ? tentativa.chave
          : crypto.randomUUID()

      tentativaPendente.current = {
        chave,
        origem,
        destino,
        valor: valorNumerico,
      }

      const transferencia = await criarTransferencia(
          origem,
          destino,
          valorNumerico,
          chave
      )

      setResultadoTransferencia(transferencia)

      // Recebemos uma resposta definitiva: a operação terminou
      tentativaPendente.current = null

    } catch (erro) {
      if (erro instanceof TypeError) {
        setErroTransferencia(
            'Não foi possível confirmar o resultado da transferência. ' +
            'Ela pode ter sido concluída. Evite realizar uma nova operação ' +
            'antes de verificar o histórico ou os saldos.'
        )
      } else {
        setErroTransferencia(
            erro instanceof Error
                ? erro.message
                : 'Não foi possível processar a transferência.'
        )
      }
    } finally {
      setTransferindo(false)
    }
  }


  async function consultarHistorico(
      evento: React.SubmitEvent<HTMLFormElement>
  ) {
    evento.preventDefault()

    const contaId = Number(idHistorico)

    if (!Number.isSafeInteger(contaId) || contaId <= 0) {
      setErroHistorico('Informe um ID de conta válido.')
      setHistorico([])
      setHistoricoCarregado(false)
      return
    }

    setConsultandoHistorico(true)
    setErroHistorico('')
    setHistorico([])
    setHistoricoCarregado(false)
    setContaHistorico(null)

    try {
      const transferencias = await buscarHistorico(contaId)

      setHistorico(
          [...transferencias].sort(
              (a, b) =>
                  new Date(b.criadaEm).getTime() -
                  new Date(a.criadaEm).getTime() ||
                  b.id - a.id
          )
      )

      setContaHistorico(contaId)
      setHistoricoCarregado(true)
    } catch (erro) {
      setErroHistorico(
          erro instanceof Error
              ? erro.message
              : 'Erro ao consultar o histórico.'
      )
    } finally {
      setConsultandoHistorico(false)
    }
  }


  return (
      <div className="app">
        <aside className="sidebar">
          <h1>BankTransfer</h1>
          <p>Sistema de transferências</p>

          <nav>
            <button onClick={() => setPagina('contas')}>
              Contas
            </button>
            <button onClick={() => setPagina('transferencias')}>
              Transferências
            </button>
            <button onClick={() => setPagina('historico')}>
              Histórico
            </button>
            <button onClick={() => setPagina('antifraude')}>
              Antifraude
            </button>
          </nav>
        </aside>

        <main className="conteudo">

          {pagina === 'contas' && (
              <section>
                <h2>Contas</h2>
                <p>Cadastre uma nova conta bancária.</p>

                <form className="formulario" onSubmit={cadastrarConta}>
                  <label htmlFor="titular">Nome do titular</label>

                  <input
                      id="titular"
                      type="text"
                      value={titular}
                      onChange={(evento) => setTitular(evento.target.value)}
                      placeholder="Digite o nome do titular"
                      disabled={carregando}
                  />

                  <label htmlFor="saldoInicial">
                    Saldo inicial de demonstração (R$)
                  </label>

                  <input
                      id="saldoInicial"
                      type="number"
                      min="0"
                      step="0.01"
                      value={saldoInicial}
                      onChange={(evento) => setSaldoInicial(evento.target.value)}
                      placeholder="0.00"
                      disabled={carregando}
                  />

                  <button type="submit" disabled={carregando}>
                    {carregando ? 'Cadastrando...' : 'Criar conta'}
                  </button>
                </form>

                {erro && <p className="mensagem-erro">{erro}</p>}

                {contaCriada && (
                    <div className="resultado-conta">
                      <h3>Conta criada com sucesso!</h3>
                      <p><strong>ID:</strong> {contaCriada.id}</p>
                      <p><strong>Titular:</strong> {contaCriada.titular}</p>
                      <p>
                        <strong>Saldo inicial:</strong>{' '}
                        {contaCriada.saldo.toLocaleString('pt-BR', {
                          style: 'currency',
                          currency: 'BRL',
                        })}
                      </p>
                    </div>
                )}


                <div className="secao-consulta">
                  <h3>Consultar conta existente</h3>

                  <form className="formulario" onSubmit={consultarConta}>
                    <label htmlFor="idConsulta">ID da conta</label>

                    <input
                        id="idConsulta"
                        type="number"
                        min="1"
                        step="1"
                        value={idConsulta}
                        onChange={(evento) => setIdConsulta(evento.target.value)}
                        placeholder="Digite o ID da conta"
                        disabled={consultando}
                    />

                    <button type="submit" disabled={consultando}>
                      {consultando ? 'Consultando...' : 'Consultar conta'}
                    </button>
                  </form>

                  {erroConsulta && <p className="mensagem-erro">{erroConsulta}</p>}

                  {contaConsultada && (
                      <div className="resultado-conta">
                        <h3>Dados da conta</h3>
                        <p><strong>ID:</strong> {contaConsultada.id}</p>
                        <p><strong>Titular:</strong> {contaConsultada.titular}</p>
                        <p>
                          <strong>Saldo:</strong>{' '}
                          {contaConsultada.saldo.toLocaleString('pt-BR', {
                            style: 'currency',
                            currency: 'BRL',
                          })}
                        </p>
                      </div>
                  )}
                </div>

              </section>
          )}

          {pagina === 'transferencias' && (
              <section>
                <h2>Transferências</h2>
                <p>Envie valores entre contas bancárias.</p>

                <form className="formulario" onSubmit={realizarTransferencia}>
                  <label htmlFor="origemId">ID da conta de origem</label>
                  <input
                      id="origemId"
                      type="number"
                      min="1"
                      step="1"
                      value={origemId}
                      onChange={(e) => setOrigemId(e.target.value)}
                      required
                  />

                  <label htmlFor="destinoId">ID da conta de destino</label>
                  <input
                      id="destinoId"
                      type="number"
                      min="1"
                      step="1"
                      value={destinoId}
                      onChange={(e) => setDestinoId(e.target.value)}
                      required
                  />

                  <label htmlFor="valor">Valor (R$)</label>
                  <input
                      id="valor"
                      type="number"
                      min="0.01"
                      step="0.01"
                      value={valor}
                      onChange={(e) => setValor(e.target.value)}
                      placeholder="0,00"
                      required
                  />

                  <button type="submit" disabled={transferindo}>
                    {transferindo ? 'Processando...' : 'Realizar transferência'}
                  </button>
                </form>

                {erroTransferencia && (
                    <p className="mensagem-erro">{erroTransferencia}</p>
                )}

                {resultadoTransferencia && (
                    <div className="resultado-conta">
                      <h3>Resultado da transferência</h3>
                      <p><strong>ID:</strong> {resultadoTransferencia.id}</p>
                      <p><strong>Status:</strong> {resultadoTransferencia.status}</p>
                      <p>
                        <strong>Valor:</strong>{' '}
                        {resultadoTransferencia.valor.toLocaleString('pt-BR', {
                          style: 'currency',
                          currency: 'BRL',
                        })}
                      </p>
                      {resultadoTransferencia.status === 'RECUSADA' && (
                          <p className="mensagem-erro">
                            Transferência bloqueada preventivamente por risco elevado.
                          </p>
                      )}
                    </div>
                )}
              </section>
          )}



          {pagina === 'historico' && (
              <section>
                <h2>Histórico de transferências</h2>
                <p>Consulte as operações enviadas e recebidas por uma conta.</p>

                <form className="formulario" onSubmit={consultarHistorico}>
                  <label htmlFor="idHistorico">ID da conta</label>

                  <input
                      id="idHistorico"
                      type="number"
                      min="1"
                      step="1"
                      value={idHistorico}
                      onChange={(e) => setIdHistorico(e.target.value)}
                      placeholder="Digite o ID da conta"
                      disabled={consultandoHistorico}
                      required
                  />

                  <button
                      key={consultandoHistorico ? 'consultando' : 'pronto'}
                      type="submit"
                      disabled={consultandoHistorico}
                  >
                    {consultandoHistorico ? 'Consultando...' : 'Consultar histórico'}
                  </button>
                </form>

                {erroHistorico && (
                    <p className="mensagem-erro">{erroHistorico}</p>
                )}

                {historicoCarregado && historico.length === 0 && (
                    <p className="mensagem-vazia">
                      Nenhuma transferência encontrada para esta conta.
                    </p>
                )}

                {historico.length > 0 && (
                    <div className="resultado-historico">
                      <h3>
                        Histórico da conta {contaHistorico}:
                      </h3>

                      <div className="tabela-container">
                      <table className="tabela-historico">
                        <thead>
                        <tr>
                          <th>ID Transferência</th>
                          <th>Tipo</th>
                          <th>ID Origem</th>
                          <th>ID Destino</th>
                          <th>Valor</th>
                          <th>Data</th>
                          <th>Status</th>
                          <th>Risco</th>
                        </tr>
                        </thead>

                        <tbody>
                        {historico.map((transferencia) => (
                            <tr key={transferencia.id}>
                              <td>{transferencia.id}</td>

                              <td>
                                {transferencia.origemId === contaHistorico
                                    ? 'Enviada'
                                    : 'Recebida'}
                              </td>


                              <td>
                                {transferencia.origemId === contaHistorico ? (
                                    <strong>{transferencia.origemId}</strong>
                                ) : (
                                    transferencia.origemId
                                )}
                              </td>

                              <td>
                                {transferencia.destinoId === contaHistorico ? (
                                    <strong>{transferencia.destinoId}</strong>
                                ) : (
                                    transferencia.destinoId
                                )}
                              </td>


                              <td>
                                {transferencia.valor.toLocaleString('pt-BR', {
                                  style: 'currency',
                                  currency: 'BRL',
                                })}
                              </td>

                              <td>
                                {new Date(transferencia.criadaEm).toLocaleString('pt-BR')}
                              </td>

                              <td>
                  <span
                      className={
                        transferencia.status === 'CONCLUIDA'
                            ? 'status-concluida'
                            : 'status-recusada'
                      }
                  >
                    {transferencia.status}
                  </span>
                              </td>

                              <td>{transferencia.nivelRisco ?? 'Não avaliado'}</td>
                            </tr>
                        ))}
                        </tbody>
                      </table>
                    </div>
                    </div>
                )}
              </section>
          )}

          {pagina === 'antifraude' && <PainelAntifraude />}

        </main>
      </div>
  )
}

export default App
