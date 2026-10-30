package br.com.ordenacao.algoritmos.serial;

import br.com.ordenacao.algoritmos.Ordenador;

import java.util.Arrays;

/**
 * Counting Sort: não compara elementos, conta quantas vezes cada valor aparece.
 * Complexidade: O(n + k), onde k = (max - min + 1) é o intervalo de valores.
 * Ótimo quando k é pequeno em relação a n; ruim quando k é enorme.
 */
public class CountingSortSerial implements Ordenador {

    /** Limite de segurança para o vetor de contagem (≈ 400 MB de int). */
    public static final long INTERVALO_MAXIMO = 100_000_000L;

    @Override public String nome() { return "CountingSort"; }
    @Override public String modo() { return "SERIAL"; }
    @Override public int threads() { return 1; }

    @Override
    public void ordenar(int[] v) {
        if (v.length < 2) return;

        int min = v[0], max = v[0];
        for (int x : v) {
            if (x < min) min = x;
            if (x > max) max = x;
        }
        long intervalo = (long) max - min + 1;
        if (intervalo > INTERVALO_MAXIMO) {
            throw new IllegalArgumentException("Intervalo de valores grande demais para Counting Sort: " + intervalo);
        }

        // 1) contagem
        int[] contagem = new int[(int) intervalo];
        for (int x : v) {
            contagem[x - min]++;
        }

        // 2) reescreve o vetor em ordem
        int pos = 0;
        for (int b = 0; b < contagem.length; b++) {
            int qtd = contagem[b];
            if (qtd > 0) {
                Arrays.fill(v, pos, pos + qtd, b + min);
                pos += qtd;
            }
        }
    }
}
