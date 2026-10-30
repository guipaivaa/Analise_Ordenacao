package br.com.ordenacao.benchmark;

import br.com.ordenacao.dados.TipoDados;

import java.util.List;
import java.util.Map;

/**
 * Tudo que pode variar num experimento fica aqui, num lugar só.
 *
 * @param tamanhos       tamanhos de vetor por algoritmo (Bubble precisa de tamanhos menores)
 * @param tipos          naturezas dos dados de entrada
 * @param valoresMaximos intervalos de valores [0, max) a testar
 * @param threads        quantidades de threads das execuções paralelas
 * @param amostras       medições válidas por cenário (o trabalho pede no mínimo 5)
 * @param aquecimentos   execuções descartadas antes de medir (aquecimento da JVM/JIT)
 * @param semente        semente fixa dos dados
 */
public record ConfigBenchmark(
        Map<Algoritmo, int[]> tamanhos,
        List<TipoDados> tipos,
        int[] valoresMaximos,
        int[] threads,
        int amostras,
        int aquecimentos,
        long semente) {
}
