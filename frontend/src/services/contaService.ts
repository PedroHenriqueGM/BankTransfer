
export type Conta = {
    id: number
    titular: string
    saldo: number
}

export async function criarConta(titular: string): Promise<Conta> {
    const resposta = await fetch('/api/contas', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
        },
        body: JSON.stringify({ titular }),
    })

    if (!resposta.ok) {
        throw new Error('Não foi possível criar a conta.')
    }

    return resposta.json()
}

export async function buscarConta(id: number): Promise<Conta> {
    const resposta = await fetch(`/api/contas/${id}`)

    if (!resposta.ok) {
        if (resposta.status === 404) {
            throw new Error('Conta não encontrada.')
        }

        throw new Error('Não foi possível consultar a conta.')
    }

    return resposta.json()
}
