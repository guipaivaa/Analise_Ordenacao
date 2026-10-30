package br.com.ordenacao.algoritmos.paralelo;

import br.com.ordenacao.algoritmos.Ordenador;
import br.com.ordenacao.algoritmos.serial.QuickSortSerial;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;

/**
 * Quick Sort paralelo com Fork/Join.
 * Depois de particionar, as duas metades são independentes, então
 * cada uma vira uma tarefa que pode rodar em outra thread.
 * Abaixo de um LIMITE de tamanho, ordena serial (criar tarefa custa caro).
 *
 * Limitação importante para o relatório: a PRIMEIRA partição percorre o vetor
 * inteiro numa thread só. Isso limita o ganho máximo (Lei de Amdahl).
 */
public class QuickSortParalelo implements Ordenador {

    private final int threads;
    private final ForkJoinPool pool;

    public QuickSortParalelo(int threads) {
        this.threads = threads;
        this.pool = new ForkJoinPool(threads);
    }

    @Override public String nome() { return "QuickSort"; }
    @Override public String modo() { return "PARALELO"; }
    @Override public int threads() { return threads; }

    @Override
    public void ordenar(int[] v) {
        if (v.length < 2) return;
        int limite = Math.max(8_192, v.length / (threads * 8));
        pool.invoke(new Tarefa(v, 0, v.length - 1, limite));
    }

    private static final class Tarefa extends RecursiveAction {
        private final int[] v;
        private final int ini, fim, limite;

        Tarefa(int[] v, int ini, int fim, int limite) {
            this.v = v; this.ini = ini; this.fim = fim; this.limite = limite;
        }

        @Override
        protected void compute() {
            if (fim - ini + 1 <= limite) {
                QuickSortSerial.ordenar(v, ini, fim);
                return;
            }
            int p = QuickSortSerial.particionar(v, ini, fim);
            invokeAll(new Tarefa(v, ini, p, limite), new Tarefa(v, p + 1, fim, limite));
        }
    }

    @Override
    public void close() {
        pool.shutdown();
    }
}
