package br.com.ordenacao.algoritmos.serial;

import br.com.ordenacao.algoritmos.Ordenador;

/**
 * Bubble Sort clássico com parada antecipada.
 * Complexidade: O(n²) no pior/médio caso, O(n) se o vetor já estiver ordenado.
 */
public class BubbleSortSerial implements Ordenador {

    @Override public String nome() { return "BubbleSort"; }
    @Override public String modo() { return "SERIAL"; }
    @Override public int threads() { return 1; }

    @Override
    public void ordenar(int[] v) {
        for (int fim = v.length - 1; fim > 0; fim--) {
            boolean trocou = false;
            for (int i = 0; i < fim; i++) {
                if (v[i] > v[i + 1]) {
                    int tmp = v[i];
                    v[i] = v[i + 1];
                    v[i + 1] = tmp;
                    trocou = true;
                }
            }
            if (!trocou) {
                return; // nenhuma troca na passada = vetor já ordenado
            }
        }
    }
}
