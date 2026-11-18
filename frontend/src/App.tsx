import { useState } from 'react'
import { buscarConta, criarConta, type Conta } from './services/contaService'
import './App.css'

type Pagina = 'contas' | 'transferencias' | 'historico' | 'antifraude'

function App() {
  const [pagina, setPagina] = useState<Pagina>('contas')
  const [titular, setTitular] = useState('')
  const [contaCriada, setContaCriada] = useState<Conta | null>(null)
  const [erro, setErro] = useState('')
  const [carregando, setCarregando] = useState(false)
  const [idConsulta, setIdConsulta] = useState('')
  const [contaConsultada, setContaConsultada] = useState<Conta | null>(null)
  const [erroConsulta, setErroConsulta] = useState('')
  const [consultando, setConsultando] = useState(false)

  async function cadastrarConta(evento: React.SubmitEvent<HTMLFormElement>) {
    evento.preventDefault()

    if (!titular.trim()) {
      setErro('Informe o nome do titular.')
      return
    }

    setCarregando(true)
    setErro('')
    setContaCriada(null)

    try {
      const conta = await criarConta(titular.trim())

      setContaCriada(conta)
      setTitular('')
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
                <p>Realize transferências entre contas.</p>
              </section>
          )}

          {pagina === 'historico' && (
              <section>
                <h2>Histórico</h2>
                <p>Consulte as transferências realizadas.</p>
              </section>
          )}

          {pagina === 'antifraude' && (
              <section>
                <h2>Antifraude</h2>
                <p>Acompanhe os alertas e níveis de risco.</p>
              </section>
          )}
        </main>
      </div>
  )
}

export default App
