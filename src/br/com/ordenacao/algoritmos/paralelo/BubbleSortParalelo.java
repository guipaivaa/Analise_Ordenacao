package br.com.ordenacao.algoritmos.paralelo;

import br.com.ordenacao.algoritmos.Ordenador;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Bubble Sort paralelo pelo método Odd-Even Transposition (ímpar-par).
 *
 * O Bubble clássico não paraleliza, porque cada comparação depende da anterior.
 * Aqui o algoritmo alterna duas fases:
 *   - fase PAR:   compara os pares (0,1), (2,3), (4,5)...
 *   - fase ÍMPAR: compara os pares (1,2), (3,4), (5,6)...
 * Dentro de uma fase os pares não se sobrepõem, então cada thread pega
 * um pedaço dos pares sem conflito. Entre uma fase e outra todas as threads
 * esperam numa barreira (CyclicBarrier). Em no máximo n fases o vetor fica ordenado.
 */
public class BubbleSortParalelo implements Ordenador {

    private final int threads;
    private final ExecutorService pool;

    public BubbleSortParalelo(int threads) {
        this.threads = threads;
        this.pool = Executors.newFixedThreadPool(threads);
    }

    @Override public String nome() { return "BubbleSort"; }
    @Override public String modo() { return "PARALELO"; }
    @Override public int threads() { return threads; }

    /** Estado compartilhado entre as threads (a barreira garante a visibilidade). */
    private static final class Estado {
        int fase = 0;
        int fasesSemTroca = 0;
        boolean parar = false;
        final boolean[] trocou;
        Estado(int t) { trocou = new boolean[t]; }
    }

    @Override
    public void ordenar(int[] v) {
        final int n = v.length;
        if (n < 2) return;

        final Estado estado = new Estado(threads);

        // A ação da barreira roda uma vez por fase, quando todas as threads chegam.
        final CyclicBarrier barreira = new CyclicBarrier(threads, () -> {
            boolean algumaTroca = false;
            for (int t = 0; t < threads; t++) {
                algumaTroca |= estado.trocou[t];
                estado.trocou[t] = false;
            }
            estado.fasesSemTroca = algumaTroca ? 0 : estado.fasesSemTroca + 1;
            estado.fase++;
            // duas fases seguidas (uma par e uma ímpar) sem trocas = ordenado
            if (estado.fasesSemTroca >= 2 || estado.fase >= n) {
                estado.parar = true;
            }
        });

        List<Callable<Void>> tarefas = new ArrayList<>();
        for (int id = 0; id < threads; id++) {
            final int t = id;
            tarefas.add(() -> {
                while (!estado.parar) {
                    int inicio = estado.fase % 2;          // 0 = fase par, 1 = fase ímpar
                    int totalPares = (n - inicio) / 2;
                    int de = (int) ((long) t * totalPares / threads);
                    int ate = (int) ((long) (t + 1) * totalPares / threads);
                    boolean trocou = false;
                    for (int p = de; p < ate; p++) {
                        int i = inicio + 2 * p;
                        if (v[i] > v[i + 1]) {
                            int tmp = v[i];
                            v[i] = v[i + 1];
                            v[i + 1] = tmp;
                            trocou = true;
                        }
                    }
                    estado.trocou[t] = trocou;
                    barreira.await();
                }
                return null;
            });
        }

        try {
            for (Future<Void> f : pool.invokeAll(tarefas)) {
                f.get();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            if (e.getCause() instanceof BrokenBarrierException) {
                throw new RuntimeException("Barreira quebrada", e.getCause());
            }
            throw new RuntimeException(e.getCause());
        }
    }

    @Override
    public void close() {
        pool.shutdownNow();
    }
}
