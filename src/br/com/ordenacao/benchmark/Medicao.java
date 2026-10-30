package br.com.ordenacao.benchmark;

import br.com.ordenacao.dados.TipoDados;

/** Uma linha de resultado bruto: uma execução de um algoritmo num cenário. */
public record Medicao(
        String algoritmo,
        String modo,
        int threads,
        TipoDados tipo,
        int tamanho,
        int valorMaximo,
        int amostra,
        long tempoNanos) {

    public double tempoMs() {
        return tempoNanos / 1_000_000.0;
    }
}
