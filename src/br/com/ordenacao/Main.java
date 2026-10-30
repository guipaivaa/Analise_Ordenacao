package br.com.ordenacao;

import br.com.ordenacao.benchmark.Algoritmo;
import br.com.ordenacao.benchmark.Benchmark;
import br.com.ordenacao.benchmark.ConfigBenchmark;
import br.com.ordenacao.benchmark.Medicao;
import br.com.ordenacao.benchmark.Resumo;
import br.com.ordenacao.csv.EscritorCSV;
import br.com.ordenacao.dados.TipoDados;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

/**
 * Ponto de entrada. Escolha o perfil pelo argumento do programa
 * (no IntelliJ: Run > Edit Configurations > Program arguments):
 *   teste    → poucos segundos, só para ver se tudo funciona
 *   rapido   → padrão; alguns minutos
 *   completo → para os gráficos finais do relatório (pode levar bastante tempo)
 *
 * Um segundo argumento, opcional, limita o número de núcleos usados:
 *   completo 4  → roda o perfil completo usando só 4 núcleos
 *   completo 8  → roda o perfil completo usando só 8 núcleos
 */
public class Main {

    public static void main(String[] args) throws Exception {
        String perfil = args.length > 0 ? args[0].toLowerCase() : "rapido";

        // "completo 4": reinicia o programa preso a 4 núcleos e espera ele terminar
        if (args.length > 1 && !LimitadorNucleos.souFilho()) {
            int codigo = LimitadorNucleos.executarLimitado(Integer.parseInt(args[1].trim()), perfil);
            System.exit(codigo);
        }
        if (LimitadorNucleos.souFilho()) {
            LimitadorNucleos.aguardarLiberacao();
        }

        ConfigBenchmark cfg = criarConfig(perfil);

        int nucleos = Runtime.getRuntime().availableProcessors();
        System.out.println("Perfil: " + perfil + " | Núcleos disponíveis: " + nucleos
                + " | Java " + System.getProperty("java.version"));

        String carimbo = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String nome = perfil + "_" + nucleos + "nucleos_" + carimbo;
        Path pasta = Path.of("resultados");
        Path brutos = pasta.resolve("medicoes_" + nome + ".csv");
        Path resumo = pasta.resolve("resumo_" + nome + ".csv");

        List<Medicao> medicoes;
        try (EscritorCSV csv = new EscritorCSV(brutos)) {
            medicoes = new Benchmark(cfg, csv).executar();
        }
        EscritorCSV.escreverResumo(resumo, Resumo.calcular(medicoes));

        System.out.println("\nMedições brutas: " + brutos.toAbsolutePath());
        System.out.println("Resumo:          " + resumo.toAbsolutePath());
    }

    private static ConfigBenchmark criarConfig(String perfil) {
        Map<Algoritmo, int[]> tamanhos = new EnumMap<>(Algoritmo.class);
        List<TipoDados> todosTipos = List.of(TipoDados.values());

        switch (perfil) {
            case "teste" -> {
                tamanhos.put(Algoritmo.BUBBLE, new int[]{500, 2_000});
                tamanhos.put(Algoritmo.QUICK, new int[]{10_000, 100_000});
                tamanhos.put(Algoritmo.MERGE, new int[]{10_000, 100_000});
                tamanhos.put(Algoritmo.COUNTING, new int[]{10_000, 100_000});
                return new ConfigBenchmark(tamanhos, List.of(TipoDados.ALEATORIO, TipoDados.INVERSO),
                        new int[]{100_000}, threadsParaTestar(), 5, 1, 42L);
            }
            case "completo" -> {
                tamanhos.put(Algoritmo.BUBBLE, new int[]{1_000, 5_000, 10_000, 20_000});
                tamanhos.put(Algoritmo.QUICK, new int[]{10_000, 100_000, 1_000_000, 5_000_000, 10_000_000});
                tamanhos.put(Algoritmo.MERGE, new int[]{10_000, 100_000, 1_000_000, 5_000_000, 10_000_000});
                tamanhos.put(Algoritmo.COUNTING, new int[]{10_000, 100_000, 1_000_000, 5_000_000, 10_000_000});
                return new ConfigBenchmark(tamanhos, todosTipos,
                        new int[]{1_000, 1_000_000}, threadsParaTestar(), 5, 2, 42L);
            }
            default -> { // rapido
                tamanhos.put(Algoritmo.BUBBLE, new int[]{1_000, 5_000, 10_000});
                tamanhos.put(Algoritmo.QUICK, new int[]{10_000, 100_000, 1_000_000});
                tamanhos.put(Algoritmo.MERGE, new int[]{10_000, 100_000, 1_000_000});
                tamanhos.put(Algoritmo.COUNTING, new int[]{10_000, 100_000, 1_000_000});
                return new ConfigBenchmark(tamanhos, todosTipos,
                        new int[]{1_000, 1_000_000}, threadsParaTestar(), 5, 1, 42L);
            }
        }
    }

    /**
     * 1, 2, 4, 8 threads + o nº de núcleos da máquina + o dobro dele.
     * Paralelo com 1 thread mostra o custo (overhead) da paralelização;
     * acima do nº de núcleos mostra que mais threads deixa de ajudar.
     */
    private static int[] threadsParaTestar() {
        int nucleos = Runtime.getRuntime().availableProcessors();
        TreeSet<Integer> ts = new TreeSet<>(List.of(1, 2, 4, 8, nucleos, nucleos * 2));
        return ts.stream().mapToInt(Integer::intValue).toArray();
    }
}