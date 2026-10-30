package br.com.ordenacao.algoritmos;

/**
 * Contrato comum a todos os algoritmos (serial e paralelo).
 * O benchmark só conhece esta interface, então qualquer algoritmo novo
 * entra no teste sem mudar o framework.
 */
public interface Ordenador extends AutoCloseable {

    /** Nome do algoritmo, ex.: "MergeSort". */
    String nome();

    /** "SERIAL" ou "PARALELO". */
    String modo();

    /** Quantidade de threads usadas (1 no serial). */
    int threads();

    /** Ordena o vetor em ordem crescente, no próprio vetor (in-place). */
    void ordenar(int[] v);

    /** Libera pools de threads. Os seriais não têm nada para liberar. */
    @Override
    default void close() {
    }
}
