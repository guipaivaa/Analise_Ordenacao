package br.com.ordenacao.algoritmos.serial;

import br.com.ordenacao.algoritmos.Ordenador;

/**
 * Quick Sort com partição de Hoare e pivô por mediana de três
 * (evita o pior caso O(n²) em vetores ordenados/invertidos).
 * Complexidade média: O(n log n).
 */
public class QuickSortSerial implements Ordenador {

    @Override public String nome() { return "QuickSort"; }
    @Override public String modo() { return "SERIAL"; }
    @Override public int threads() { return 1; }

    @Override
    public void ordenar(int[] v) {
        ordenar(v, 0, v.length - 1);
    }

    /** Público para ser reaproveitado pela versão paralela nos pedaços pequenos. */
    public static void ordenar(int[] v, int ini, int fim) {
        // Recursão só no lado menor e laço no maior: pilha fica em O(log n).
        while (ini < fim) {
            int p = particionar(v, ini, fim);
            if (p - ini < fim - p) {
                ordenar(v, ini, p);
                ini = p + 1;
            } else {
                ordenar(v, p + 1, fim);
                fim = p;
            }
        }
    }

    /**
     * Partição de Hoare. Ao final, todo elemento em [ini, j] é <= pivô
     * e todo elemento em [j+1, fim] é >= pivô. Retorna j.
     */
    public static int particionar(int[] v, int ini, int fim) {
        int meio = (ini + fim) >>> 1;
        // mediana de três: deixa ini <= meio <= fim
        if (v[meio] < v[ini]) trocar(v, meio, ini);
        if (v[fim] < v[ini]) trocar(v, fim, ini);
        if (v[fim] < v[meio]) trocar(v, fim, meio);
        int pivo = v[meio];

        int i = ini - 1;
        int j = fim + 1;
        while (true) {
            do { i++; } while (v[i] < pivo);
            do { j--; } while (v[j] > pivo);
            if (i >= j) return j;
            trocar(v, i, j);
        }
    }

    private static void trocar(int[] v, int a, int b) {
        int tmp = v[a];
        v[a] = v[b];
        v[b] = tmp;
    }
}
