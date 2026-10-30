package br.com.ordenacao.dados;

/** A "natureza" do conjunto de entrada, que muda muito o desempenho de alguns algoritmos. */
public enum TipoDados {
    /** Valores sorteados uniformemente: caso médio. */
    ALEATORIO,
    /** Já em ordem crescente: melhor caso do Bubble (com parada antecipada). */
    ORDENADO,
    /** Ordem decrescente: pior caso do Bubble (máximo de trocas). */
    INVERSO,
    /** Ordenado com ~5% de posições trocadas: cenário comum na prática. */
    QUASE_ORDENADO
}
