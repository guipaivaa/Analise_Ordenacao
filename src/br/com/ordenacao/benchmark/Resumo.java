package br.com.ordenacao.benchmark;

import br.com.ordenacao.dados.TipoDados;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Análise estatística: agrupa as amostras de cada cenário e calcula
 * média, desvio padrão, mínimo, máximo, speedup e eficiência.
 *
 * speedup    = tempo médio SERIAL / tempo médio PARALELO (mesmo algoritmo, tipo, tamanho e intervalo)
 * eficiência = speedup / threads   (1,0 = aproveitou 100% das threads)
 */
public final class Resumo {

    private Resumo() {
    }

    public record Linha(
            String algoritmo, String modo, int threads, TipoDados tipo, int tamanho, int valorMaximo,
            int amostras, double mediaMs, double desvioMs, double minMs, double maxMs,
            double speedup, double eficiencia) {
    }

    private record Chave(String algoritmo, String modo, int threads, TipoDados tipo, int tamanho, int valorMaximo) {
    }

    private record Cenario(String algoritmo, TipoDados tipo, int tamanho, int valorMaximo) {
    }

    public static List<Linha> calcular(List<Medicao> medicoes) {
        Map<Chave, List<Double>> grupos = new LinkedHashMap<>();
        for (Medicao m : medicoes) {
            Chave c = new Chave(m.algoritmo(), m.modo(), m.threads(), m.tipo(), m.tamanho(), m.valorMaximo());
            grupos.computeIfAbsent(c, x -> new ArrayList<>()).add(m.tempoMs());
        }

        // média serial de cada cenário: referência para o speedup
        Map<Cenario, Double> mediaSerial = new LinkedHashMap<>();
        grupos.forEach((c, tempos) -> {
            if (c.modo().equals("SERIAL")) {
                mediaSerial.put(new Cenario(c.algoritmo(), c.tipo(), c.tamanho(), c.valorMaximo()), media(tempos));
            }
        });

        List<Linha> linhas = new ArrayList<>();
        grupos.forEach((c, tempos) -> {
            double media = media(tempos);
            Double serial = mediaSerial.get(new Cenario(c.algoritmo(), c.tipo(), c.tamanho(), c.valorMaximo()));
            double speedup = (serial == null || media == 0) ? Double.NaN : serial / media;
            double eficiencia = speedup / c.threads();
            linhas.add(new Linha(c.algoritmo(), c.modo(), c.threads(), c.tipo(), c.tamanho(), c.valorMaximo(),
                    tempos.size(), media, desvioPadrao(tempos, media),
                    tempos.stream().mapToDouble(Double::doubleValue).min().orElse(0),
                    tempos.stream().mapToDouble(Double::doubleValue).max().orElse(0),
                    speedup, eficiencia));
        });
        return linhas;
    }

    private static double media(List<Double> xs) {
        return xs.stream().mapToDouble(Double::doubleValue).average().orElse(0);
    }

    /** Desvio padrão amostral (divide por n-1). */
    private static double desvioPadrao(List<Double> xs, double media) {
        if (xs.size() < 2) return 0;
        double soma = 0;
        for (double x : xs) soma += (x - media) * (x - media);
        return Math.sqrt(soma / (xs.size() - 1));
    }
}
