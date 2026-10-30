package br.com.ordenacao.algoritmos.paralelo;

import br.com.ordenacao.algoritmos.Ordenador;
import br.com.ordenacao.algoritmos.serial.CountingSortSerial;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.IntConsumer;

/**
 * Counting Sort paralelo. Cada etapa é dividida entre as threads:
 *   1) cada thread acha min/max do seu pedaço do vetor;
 *   2) cada thread conta os valores do seu pedaço num vetor de contagem LOCAL
 *      (local para não haver disputa/sincronização por posição);
 *   3) as contagens locais são somadas, cada thread cuidando de uma faixa de valores;
 *   4) soma de prefixos (serial, O(k)) para saber onde cada valor começa;
 *   5) cada thread reescreve no vetor a sua faixa de valores.
 */
public class CountingSortParalelo implements Ordenador {

    private final int threads;
    private final ExecutorService pool;

    public CountingSortParalelo(int threads) {
        this.threads = threads;
        this.pool = Executors.newFixedThreadPool(threads);
    }

    @Override public String nome() { return "CountingSort"; }
    @Override public String modo() { return "PARALELO"; }
    @Override public int threads() { return threads; }

    @Override
    public void ordenar(int[] v) {
        final int n = v.length;
        if (n < 2) return;
        final int t = threads;

        // 1) min e max em paralelo
        final int[] mins = new int[t];
        final int[] maxs = new int[t];
        executar(id -> {
            int mn = Integer.MAX_VALUE, mx = Integer.MIN_VALUE;
            for (int i = inicio(id, n, t), fim = inicio(id + 1, n, t); i < fim; i++) {
                if (v[i] < mn) mn = v[i];
                if (v[i] > mx) mx = v[i];
            }
            mins[id] = mn;
            maxs[id] = mx;
        });
        int min = Arrays.stream(mins).min().getAsInt();
        int max = Arrays.stream(maxs).max().getAsInt();
        long intervalo = (long) max - min + 1;
        if (intervalo > CountingSortSerial.INTERVALO_MAXIMO) {
            throw new IllegalArgumentException("Intervalo de valores grande demais para Counting Sort: " + intervalo);
        }
        final int k = (int) intervalo;
        final int menor = min;

        // 2) contagem local por thread. Cada thread aloca (e zera) o próprio vetor:
        //    se a thread principal alocasse os t vetores, o custo O(t·k) ficaria serial.
        final int[][] locais = new int[t][];
        executar(id -> {
            int[] c = new int[k];
            locais[id] = c;
            for (int i = inicio(id, n, t), fim = inicio(id + 1, n, t); i < fim; i++) {
                c[v[i] - menor]++;
            }
        });

        // 3) soma das contagens, dividindo a faixa de valores entre as threads
        final int[] total = new int[k];
        executar(id -> {
            for (int b = inicio(id, k, t), fim = inicio(id + 1, k, t); b < fim; b++) {
                int s = 0;
                for (int[] c : locais) s += c[b];
                total[b] = s;
            }
        });

        // 4) prefixo: posição inicial de cada valor no vetor final
        final int[] posicao = new int[k];
        int acumulado = 0;
        for (int b = 0; b < k; b++) {
            posicao[b] = acumulado;
            acumulado += total[b];
        }

        // 5) cada thread escreve a sua faixa de valores
        executar(id -> {
            for (int b = inicio(id, k, t), fim = inicio(id + 1, k, t); b < fim; b++) {
                if (total[b] > 0) {
                    Arrays.fill(v, posicao[b], posicao[b] + total[b], b + menor);
                }
            }
        });
    }

    /** Índice inicial do bloco 'id' quando 'tamanho' é dividido em 't' blocos. */
    private static int inicio(int id, int tamanho, int t) {
        return (int) ((long) id * tamanho / t);
    }

    /** Roda a mesma etapa em todas as threads e espera todas terminarem. */
    private void executar(IntConsumer etapa) {
        List<Callable<Void>> tarefas = new ArrayList<>(threads);
        for (int id = 0; id < threads; id++) {
            final int i = id;
            tarefas.add(() -> { etapa.accept(i); return null; });
        }
        try {
            for (Future<Void> f : pool.invokeAll(tarefas)) {
                f.get();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            throw new RuntimeException(e.getCause());
        }
    }

    @Override
    public void close() {
        pool.shutdownNow();
    }
}
