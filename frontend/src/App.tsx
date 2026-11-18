
import { useState } from 'react'
import './App.css'

type Pagina = 'contas' | 'transferencias' | 'historico' | 'antifraude'

function App() {
  const [pagina, setPagina] = useState<Pagina>('contas')

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
                <p>Cadastre contas e consulte seus saldos.</p>
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
