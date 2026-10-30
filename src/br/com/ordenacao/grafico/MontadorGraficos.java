package br.com.ordenacao.grafico;

import br.com.ordenacao.grafico.DadosResumo.Ponto;
import br.com.ordenacao.grafico.GraficoPanel.Serie;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Transforma as linhas do resumo nas séries de cada tipo de gráfico.
 * Separado da janela para poder gerar os gráficos também sem interface.
 */
public final class MontadorGraficos {

    private MontadorGraficos() {
    }

    /**
     * Configura o painel com o gráfico pedido.
     *
     * @return nome sugerido para o arquivo PNG
     */
    public static String montar(GraficoPanel painel, TipoGrafico tipoGrafico, List<Ponto> dados,
                                String algoritmo, String tipoDados, int valorMaximo, int nucleos,
                                boolean tempoLog) {

        if (tipoGrafico.comparaNucleos()) {
            return montarComparacaoNucleos(painel, tipoGrafico, dados, algoritmo, tipoDados, valorMaximo);
        }

        String filtro = "dados " + tipoDados + "  ·  valores de 0 a " + GraficoPanel.formatar(valorMaximo)
                + (nucleos > 0 ? "  ·  " + nucleos + " núcleos" : "");

        List<Ponto> doCenario = dados.stream()
                .filter(p -> p.nucleos() == nucleos && p.tipo().equals(tipoDados) && p.valorMaximo() == valorMaximo)
                .toList();
        List<Ponto> sel = doCenario.stream()
                .filter(p -> p.algoritmo().equals(algoritmo))
                .sorted(Comparator.comparing((Ponto p) -> !p.serial())
                        .thenComparingInt(Ponto::threads)
                        .thenComparingInt(Ponto::tamanho))
                .toList();

        String nomeArquivo = (tipoGrafico.name() + "_" + algoritmo + "_" + tipoDados + "_ate" + valorMaximo
                + (nucleos > 0 ? "_" + nucleos + "nucleos" : "")).toLowerCase();

        switch (tipoGrafico) {
            case TEMPO_TAMANHO -> {
                Map<String, List<double[]>> m = new LinkedHashMap<>();
                for (Ponto p : sel) {
                    m.computeIfAbsent(p.nomeSerie(), k -> new ArrayList<>()).add(new double[]{p.tamanho(), p.mediaMs()});
                }
                painel.mostrar(algoritmo + " — tempo médio × tamanho do vetor", filtro,
                        "Tamanho do vetor (n)", "Tempo médio (ms)", " ms",
                        series(m, "Serial"), null, true, tempoLog, null, null);
            }
            case TEMPO_THREADS -> {
                List<String> categorias = new ArrayList<>();
                categorias.add("Serial");
                List<Integer> threads = threadsParalelas(sel);
                threads.forEach(t -> categorias.add(String.valueOf(t)));

                Map<String, List<double[]>> m = new LinkedHashMap<>();
                for (Ponto p : ordenadoPorTamanho(sel)) {
                    double x = p.serial() ? 0 : threads.indexOf(p.threads()) + 1;
                    m.computeIfAbsent(nomeTamanho(p), k -> new ArrayList<>()).add(new double[]{x, p.mediaMs()});
                }
                painel.mostrar(algoritmo + " — tempo médio × número de threads", filtro,
                        "Threads", "Tempo médio (ms)", " ms",
                        series(m, null), categorias, false, tempoLog, null, null);
            }
            case SPEEDUP, EFICIENCIA -> {
                boolean speedup = tipoGrafico == TipoGrafico.SPEEDUP;
                List<Ponto> paralelos = sel.stream().filter(p -> !p.serial()).toList();
                List<Integer> threads = threadsParalelas(paralelos);
                List<String> categorias = threads.stream().map(String::valueOf).toList();

                Map<String, List<double[]>> m = new LinkedHashMap<>();
                for (Ponto p : ordenadoPorTamanho(paralelos)) {
                    double y = speedup ? p.speedup() : p.eficiencia();
                    m.computeIfAbsent(nomeTamanho(p), k -> new ArrayList<>())
                            .add(new double[]{threads.indexOf(p.threads()), y});
                }
                if (speedup) {
                    painel.mostrar(algoritmo + " — speedup × número de threads", filtro,
                            "Threads", "Speedup (tempo serial ÷ tempo paralelo)", "×",
                            series(m, null), categorias, false, false, 1.0, "Serial (speedup = 1)");
                } else {
                    painel.mostrar(algoritmo + " — eficiência × número de threads", filtro,
                            "Threads", "Eficiência (speedup ÷ threads)", "",
                            series(m, null), categorias, false, false, 1.0, "Eficiência ideal (1,0)");
                }
            }
            case COMPARAR -> {
                // por algoritmo: curva serial e curva com o melhor nº de threads de cada tamanho
                Map<String, List<double[]>> m = new LinkedHashMap<>();
                List<String> serieSerial = new ArrayList<>();
                Map<String, List<Ponto>> porAlgoritmo = new LinkedHashMap<>();
                doCenario.forEach(p -> porAlgoritmo.computeIfAbsent(p.algoritmo(), k -> new ArrayList<>()).add(p));

                for (var e : porAlgoritmo.entrySet()) {
                    String nomeSerial = e.getKey() + " serial";
                    String nomeParalelo = e.getKey() + " paralelo (melhor nº de threads)";
                    serieSerial.add(nomeSerial);
                    Map<Integer, Double> serial = new TreeMap<>();
                    Map<Integer, Double> melhor = new TreeMap<>();
                    for (Ponto p : e.getValue()) {
                        if (p.serial()) serial.put(p.tamanho(), p.mediaMs());
                        else melhor.merge(p.tamanho(), p.mediaMs(), Math::min);
                    }
                    m.put(nomeSerial, pontos(serial));
                    m.put(nomeParalelo, pontos(melhor));
                }
                // mesma cor para serial (tracejado) e paralelo (contínuo) de cada algoritmo
                List<Serie> lista = new ArrayList<>();
                int i = 0;
                for (var e : m.entrySet()) {
                    lista.add(new Serie(e.getKey(), e.getValue(), serieSerial.contains(e.getKey()), i / 2));
                    i++;
                }
                painel.mostrar("Comparação entre algoritmos — tempo médio × tamanho", filtro,
                        "Tamanho do vetor (n)", "Tempo médio (ms)", " ms",
                        lista, null, true, tempoLog, null, null);
                nomeArquivo = ("comparar_" + tipoDados + "_ate" + valorMaximo
                        + (nucleos > 0 ? "_" + nucleos + "nucleos" : "")).toLowerCase();
            }
            default -> throw new IllegalStateException("Tipo de gráfico não tratado: " + tipoGrafico);
        }
        return nomeArquivo;
    }

    /**
     * Gráficos do objetivo 3 (variação do número de núcleos). Juntam as rodadas de 4, 8 e 12 núcleos
     * e usam o MAIOR tamanho de vetor de cada algoritmo, onde o paralelismo fica mais visível.
     */
    private static String montarComparacaoNucleos(GraficoPanel painel, TipoGrafico tipoGrafico, List<Ponto> dados,
                                                  String algoritmo, String tipoDados, int valorMaximo) {
        List<Ponto> doCenario = dados.stream()
                .filter(p -> p.nucleos() > 0 && !p.serial()
                        && p.tipo().equals(tipoDados) && p.valorMaximo() == valorMaximo)
                .toList();
        String filtro = "dados " + tipoDados + "  ·  valores de 0 a " + GraficoPanel.formatar(valorMaximo);
        List<Integer> nucleos = doCenario.stream().map(Ponto::nucleos).distinct().sorted().toList();

        if (tipoGrafico == TipoGrafico.SPEEDUP_NUCLEOS) {
            List<Ponto> doAlg = doCenario.stream().filter(p -> p.algoritmo().equals(algoritmo)).toList();
            int maiorN = doAlg.stream().mapToInt(Ponto::tamanho).max().orElse(0);
            List<Ponto> sel = doAlg.stream().filter(p -> p.tamanho() == maiorN).toList();
            List<Integer> threads = sel.stream().map(Ponto::threads).distinct().sorted().toList();
            List<String> categorias = threads.stream().map(String::valueOf).toList();

            List<Serie> series = new ArrayList<>();
            for (int nuc : nucleos) {
                List<double[]> pts = new ArrayList<>();
                sel.stream().filter(p -> p.nucleos() == nuc)
                        .forEach(p -> pts.add(new double[]{threads.indexOf(p.threads()), p.speedup()}));
                series.add(new Serie(nuc + " núcleos", pts, false));
            }
            painel.mostrar(algoritmo + " — speedup × threads, com 4, 8 e 12 núcleos",
                    filtro + "  ·  n = " + GraficoPanel.formatarTamanho(maiorN),
                    "Threads", "Speedup (tempo serial ÷ tempo paralelo)", "×",
                    series, categorias, false, false, 1.0, "Serial (speedup = 1)");
            return ("speedup_nucleos_" + algoritmo + "_" + tipoDados + "_ate" + valorMaximo).toLowerCase();
        }

        // MELHOR_NUCLEOS: uma linha por algoritmo; em cada nº de núcleos, o melhor speedup obtido
        List<String> categorias = nucleos.stream().map(String::valueOf).toList();
        Map<String, List<double[]>> m = new LinkedHashMap<>();
        Map<String, List<Ponto>> porAlgoritmo = new LinkedHashMap<>();
        doCenario.forEach(p -> porAlgoritmo.computeIfAbsent(p.algoritmo(), k -> new ArrayList<>()).add(p));
        for (var e : porAlgoritmo.entrySet()) {
            int maiorN = e.getValue().stream().mapToInt(Ponto::tamanho).max().orElse(0);
            List<double[]> pts = new ArrayList<>();
            for (int i = 0; i < nucleos.size(); i++) {
                int nuc = nucleos.get(i);
                e.getValue().stream()
                        .filter(p -> p.nucleos() == nuc && p.tamanho() == maiorN)
                        .mapToDouble(Ponto::speedup).max()
                        .ifPresent(v -> pts.add(new double[]{nucleos.indexOf(nuc), v}));
            }
            m.put(e.getKey() + " (n = " + GraficoPanel.formatarTamanho(maiorN) + ")", pts);
        }
        painel.mostrar("Melhor speedup de cada algoritmo × número de núcleos", filtro,
                "Núcleos disponíveis", "Melhor speedup obtido", "×",
                series(m, null), categorias, false, false, 1.0, "Serial (speedup = 1)");
        return ("melhor_speedup_nucleos_" + tipoDados + "_ate" + valorMaximo).toLowerCase();
    }

    private static List<Serie> series(Map<String, List<double[]>> m, String tracejada) {
        List<Serie> lista = new ArrayList<>();
        m.forEach((nome, pts) -> lista.add(new Serie(nome, pts, nome.equals(tracejada))));
        return lista;
    }

    private static List<double[]> pontos(Map<Integer, Double> m) {
        List<double[]> pts = new ArrayList<>();
        m.forEach((x, y) -> pts.add(new double[]{x, y}));
        return pts;
    }

    private static List<Integer> threadsParalelas(List<Ponto> pontos) {
        return pontos.stream().filter(p -> !p.serial()).map(Ponto::threads).distinct().sorted().toList();
    }

    private static List<Ponto> ordenadoPorTamanho(List<Ponto> pontos) {
        return pontos.stream().sorted(Comparator.comparingInt(Ponto::tamanho)).toList();
    }

    private static String nomeTamanho(Ponto p) {
        return "n = " + GraficoPanel.formatarTamanho(p.tamanho());
    }
}