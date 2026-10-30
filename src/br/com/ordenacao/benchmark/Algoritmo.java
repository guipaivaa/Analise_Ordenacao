package br.com.ordenacao.benchmark;

import br.com.ordenacao.algoritmos.Ordenador;
import br.com.ordenacao.algoritmos.paralelo.BubbleSortParalelo;
import br.com.ordenacao.algoritmos.paralelo.CountingSortParalelo;
import br.com.ordenacao.algoritmos.paralelo.MergeSortParalelo;
import br.com.ordenacao.algoritmos.paralelo.QuickSortParalelo;
import br.com.ordenacao.algoritmos.serial.BubbleSortSerial;
import br.com.ordenacao.algoritmos.serial.CountingSortSerial;
import br.com.ordenacao.algoritmos.serial.MergeSortSerial;
import br.com.ordenacao.algoritmos.serial.QuickSortSerial;

/**
 * Fábrica: liga o nome do algoritmo às suas duas implementações.
 * Para trocar um algoritmo (ex.: Insertion no lugar de Bubble),
 * basta criar as duas classes e adicionar uma linha aqui.
 */
public enum Algoritmo {
    BUBBLE {
        @Override public Ordenador serial() { return new BubbleSortSerial(); }
        @Override public Ordenador paralelo(int t) { return new BubbleSortParalelo(t); }
    },
    QUICK {
        @Override public Ordenador serial() { return new QuickSortSerial(); }
        @Override public Ordenador paralelo(int t) { return new QuickSortParalelo(t); }
    },
    MERGE {
        @Override public Ordenador serial() { return new MergeSortSerial(); }
        @Override public Ordenador paralelo(int t) { return new MergeSortParalelo(t); }
    },
    COUNTING {
        @Override public Ordenador serial() { return new CountingSortSerial(); }
        @Override public Ordenador paralelo(int t) { return new CountingSortParalelo(t); }
    };

    public abstract Ordenador serial();

    public abstract Ordenador paralelo(int threads);
}
