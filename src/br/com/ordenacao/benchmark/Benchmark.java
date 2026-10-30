package br.com.ordenacao.benchmark;

import br.com.ordenacao.algoritmos.Ordenador;
import br.com.ordenacao.csv.EscritorCSV;
import br.com.ordenacao.dados.GeradorDados;
import br.com.ordenacao.dados.TipoDados;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * O "framework de teste". Para cada cenário (algoritmo × tipo × tamanho × intervalo):
 *   1. gera o vetor base uma única vez (mesmos dados para serial e paralelo);
 *   2. roda a versão SERIAL e a PARALELA com cada quantidade de threads;
 *   3. em cada execução: copia o vetor (fora do cronômetro), mede só a ordenação,
 *      e confere se o resultado está correto (também fora do cronômetro).
 */
public class Benchmark {

    private final ConfigBenchmark cfg;
    private final EscritorCSV csv;
    private final List<Medicao> medicoes = new ArrayList<>();

    public Benchmark(ConfigBenchmark cfg, EscritorCSV csv) {
        this.cfg = cfg;
        this.csv = csv;
    }

    public List<Medicao> executar() {
        for (Map.Entry<Algoritmo, int[]> entrada : cfg.tamanhos().entrySet()) {
            Algoritmo alg = entrada.getKey();
            for (int valorMaximo : cfg.valoresMaximos()) {
                for (TipoDados tipo : cfg.tipos()) {
                    for (int tamanho : entrada.getValue()) {
                        int[] base = GeradorDados.gerar(tamanho, tipo, valorMaximo, cfg.semente());
                        int[] esperado = base.clone();
                        Arrays.sort(esperado); // gabarito para validar

                        try (Ordenador o = alg.serial()) {
                            medir(o, base, esperado, tipo, valorMaximo);
                        }
                        for (int t : cfg.threads()) {
                            try (Ordenador o = alg.paralelo(t)) {
                                medir(o, base, esperado, tipo, valorMaximo);
                            }
                        }
                    }
                }
            }
        }
        return medicoes;
    }

    private void medir(Ordenador o, int[] base, int[] esperado, TipoDados tipo, int valorMaximo) {
        // aquecimento: deixa o JIT compilar o código antes de medir (resultados descartados)
        for (int i = 0; i < cfg.aquecimentos(); i++) {
            o.ordenar(base.clone());
        }

        double soma = 0;
        for (int amostra = 1; amostra <= cfg.amostras(); amostra++) {
            int[] v = base.clone();

            long ini = System.nanoTime();
            o.ordenar(v);
            long tempo = System.nanoTime() - ini;

            if (!Arrays.equals(v, esperado)) {
                throw new IllegalStateException("Resultado INCORRETO: " + o.nome() + " " + o.modo()
                        + " threads=" + o.threads() + " tipo=" + tipo + " n=" + base.length);
            }

            Medicao m = new Medicao(o.nome(), o.modo(), o.threads(), tipo, base.length, valorMaximo, amostra, tempo);
            medicoes.add(m);
            csv.escrever(m);
            soma += m.tempoMs();
        }

        System.out.printf("%-12s %-8s threads=%-2d %-14s n=%-10d max=%-10d media=%10.3f ms%n",
                o.nome(), o.modo(), o.threads(), tipo, base.length, valorMaximo, soma / cfg.amostras());
    }
}
