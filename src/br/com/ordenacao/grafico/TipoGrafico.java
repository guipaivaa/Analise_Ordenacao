package br.com.ordenacao.grafico;

/** Os gráficos que o visualizador sabe desenhar. */
public enum TipoGrafico {
    TEMPO_TAMANHO("Tempo × Tamanho"),
    TEMPO_THREADS("Tempo × Threads"),
    SPEEDUP("Speedup × Threads"),
    EFICIENCIA("Eficiência × Threads"),
    COMPARAR("Comparar algoritmos"),
    SPEEDUP_NUCLEOS("Speedup × Threads (por nº de núcleos)"),
    MELHOR_NUCLEOS("Melhor speedup × Núcleos");

    /** true para os gráficos que juntam as rodadas de núcleos diferentes. */
    public boolean comparaNucleos() {
        return this == SPEEDUP_NUCLEOS || this == MELHOR_NUCLEOS;
    }

    private final String rotulo;

    TipoGrafico(String rotulo) {
        this.rotulo = rotulo;
    }

    @Override
    public String toString() {
        return rotulo;
    }
}