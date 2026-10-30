package br.com.ordenacao.algoritmos.paralelo;

import br.com.ordenacao.algoritmos.Ordenador;
import br.com.ordenacao.algoritmos.serial.MergeSortSerial;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;

/**
 * Merge Sort paralelo com Fork/Join.
 * As duas metades são ordenadas em paralelo (fork) e depois intercaladas (join).
 * Abaixo de um LIMITE de tamanho, usa a versão serial.
 *
 * Limitação para o relatório: a ÚLTIMA intercalação (do vetor inteiro)
 * é feita por uma thread só, o que limita o speedup.
 */
public class MergeSortParalelo implements Ordenador {

    private final int threads;
    private final ForkJoinPool pool;

    public MergeSortParalelo(int threads) {
        this.threads = threads;
        this.pool = new ForkJoinPool(threads);
    }

    @Override public String nome() { return "MergeSort"; }
    @Override public String modo() { return "PARALELO"; }
    @Override public int threads() { return threads; }

    @Override
    public void ordenar(int[] v) {
        if (v.length < 2) return;
        int[] aux = new int[v.length];
        int limite = Math.max(8_192, v.length / (threads * 8));
        pool.invoke(new Tarefa(v, aux, 0, v.length - 1, limite));
    }

    private static final class Tarefa extends RecursiveAction {
        private final int[] v, aux;
        private final int ini, fim, limite;

        Tarefa(int[] v, int[] aux, int ini, int fim, int limite) {
            this.v = v; this.aux = aux; this.ini = ini; this.fim = fim; this.limite = limite;
        }

        @Override
        protected void compute() {
            if (fim - ini + 1 <= limite) {
                MergeSortSerial.ordenar(v, aux, ini, fim);
                return;
            }
            int meio = (ini + fim) >>> 1;
            invokeAll(new Tarefa(v, aux, ini, meio, limite),
                      new Tarefa(v, aux, meio + 1, fim, limite));
            MergeSortSerial.intercalar(v, aux, ini, meio, fim);
        }
    }

    @Override
    public void close() {
        pool.shutdown();
    }
}
