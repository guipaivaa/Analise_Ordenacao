package br.com.ordenacao.algoritmos.serial;

import br.com.ordenacao.algoritmos.Ordenador;

/**
 * Merge Sort top-down (divide ao meio, ordena cada metade, intercala).
 * Complexidade: O(n log n) em todos os casos. Usa O(n) de memória extra.
 */
public class MergeSortSerial implements Ordenador {

    @Override public String nome() { return "MergeSort"; }
    @Override public String modo() { return "SERIAL"; }
    @Override public int threads() { return 1; }

    @Override
    public void ordenar(int[] v) {
        if (v.length < 2) return;
        int[] aux = new int[v.length]; // alocado uma vez só
        ordenar(v, aux, 0, v.length - 1);
    }

    public static void ordenar(int[] v, int[] aux, int ini, int fim) {
        if (ini >= fim) return;
        int meio = (ini + fim) >>> 1;
        ordenar(v, aux, ini, meio);
        ordenar(v, aux, meio + 1, fim);
        intercalar(v, aux, ini, meio, fim);
    }

    /** Junta duas metades já ordenadas: [ini, meio] e [meio+1, fim]. */
    public static void intercalar(int[] v, int[] aux, int ini, int meio, int fim) {
        if (v[meio] <= v[meio + 1]) return; // já estão em ordem
        System.arraycopy(v, ini, aux, ini, fim - ini + 1);
        int i = ini, j = meio + 1;
        for (int k = ini; k <= fim; k++) {
            if (i > meio)              v[k] = aux[j++];
            else if (j > fim)          v[k] = aux[i++];
            else if (aux[j] < aux[i])  v[k] = aux[j++];
            else                       v[k] = aux[i++];
        }
    }
}
