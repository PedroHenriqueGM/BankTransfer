
export type Transferencia = {
    id: number
    origemId: number
    destinoId: number
    valor: number
    status: 'CONCLUIDA' | 'RECUSADA'
    criadaEm: string
    pontuacaoRisco: number | null
    nivelRisco: 'BAIXO' | 'MODERADO' | 'ALTO' | null
}

export async function criarTransferencia(
    origemId: number,
    destinoId: number,
    valor: number,
    chaveIdempotencia: string
): Promise<Transferencia> {
    const resposta = await fetch('/api/transferencias', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            'Idempotency-Key': chaveIdempotencia,
        },
        body: JSON.stringify({ origemId, destinoId, valor }),
    })

    if (!resposta.ok) {
        throw new Error('Não foi possível processar a transferência.')
    }

    return resposta.json()
}

export async function buscarHistorico(
    contaId: number
): Promise<Transferencia[]> {
    const resposta = await fetch(
        `/api/contas/${contaId}/transferencias`
    )

    if (!resposta.ok) {
        if (resposta.status === 404) {
            throw new Error('Conta não encontrada.')
        }

        throw new Error('Não foi possível consultar o histórico.')
    }

    return resposta.json()
}
