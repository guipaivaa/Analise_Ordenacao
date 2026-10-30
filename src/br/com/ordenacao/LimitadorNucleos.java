package br.com.ordenacao;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Limita o benchmark a N núcleos de verdade (objetivo 3 do trabalho).
 *
 * O Java não consegue restringir os núcleos do próprio processo. Por isso o Main
 * se reinicia como um processo "filho" e o sistema operacional fixa esse filho
 * nos N primeiros núcleos (afinidade de processador):
 *   - Windows: PowerShell, propriedade ProcessorAffinity;
 *   - Linux:   comando taskset.
 * O filho também recebe -XX:ActiveProcessorCount=N, para que o Java informe N
 * núcleos (isso define as threads testadas e a coluna nucleos_disponiveis do CSV).
 *
 * O filho espera um sinal antes de começar, para garantir que a afinidade
 * já foi aplicada quando a primeira medição acontecer.
 */
public final class LimitadorNucleos {

    private static final String PROPRIEDADE_FILHO = "ordenacao.filho";

    private LimitadorNucleos() {
    }

    /** true se este processo é o filho já limitado. */
    public static boolean souFilho() {
        return System.getProperty(PROPRIEDADE_FILHO) != null;
    }

    /** No filho: espera o pai confirmar que a afinidade foi aplicada. */
    public static void aguardarLiberacao() {
        try {
            new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8)).readLine();
        } catch (IOException e) {
            throw new IllegalStateException("Falha ao aguardar o processo principal", e);
        }
    }

    /**
     * No pai: reinicia o programa limitado a 'nucleos' núcleos e espera terminar.
     *
     * @return código de saída do filho
     */
    public static int executarLimitado(int nucleos, String perfil) throws IOException, InterruptedException {
        int total = Runtime.getRuntime().availableProcessors();
        if (nucleos < 1 || nucleos > total) {
            throw new IllegalArgumentException("Número de núcleos inválido: " + nucleos
                    + " (esta máquina tem " + total + ").");
        }

        String java = ProcessHandle.current().info().command()
                .orElse(System.getProperty("java.home") + "/bin/java");

        List<String> comando = new ArrayList<>();
        comando.add(java);
        comando.add("-XX:ActiveProcessorCount=" + nucleos);
        comando.add("-D" + PROPRIEDADE_FILHO + "=true");
        // repassa as opções de codificação que o IntelliJ usa, para os acentos saírem certos
        for (String arg : ManagementFactory.getRuntimeMXBean().getInputArguments()) {
            if (arg.startsWith("-Dfile.encoding") || arg.startsWith("-Dstdout.encoding")
                    || arg.startsWith("-Dstderr.encoding") || arg.startsWith("-Dsun.stdout.encoding")
                    || arg.startsWith("-Dsun.stderr.encoding")) {
                comando.add(arg);
            }
        }
        comando.add("-cp");
        comando.add(System.getProperty("java.class.path"));
        comando.add(Main.class.getName());
        comando.add(perfil);

        Process filho = new ProcessBuilder(comando)
                .redirectOutput(ProcessBuilder.Redirect.INHERIT)
                .redirectError(ProcessBuilder.Redirect.INHERIT)
                .start();
        // se você clicar em Stop no IntelliJ, o filho é encerrado junto
        Runtime.getRuntime().addShutdownHook(new Thread(filho::destroyForcibly));

        try {
            aplicarAfinidade(filho.pid(), nucleos);
        } catch (IOException | RuntimeException e) {
            filho.destroyForcibly();
            throw e;
        }

        System.out.println("Benchmark fixado nos núcleos 0 a " + (nucleos - 1) + " de " + total + ".\n");
        try (OutputStream sinal = filho.getOutputStream()) {
            sinal.write("ok\n".getBytes(StandardCharsets.UTF_8));
        }
        return filho.waitFor();
    }

    private static void aplicarAfinidade(long pid, int nucleos) throws IOException, InterruptedException {
        long mascara = nucleos >= 63 ? Long.MAX_VALUE : (1L << nucleos) - 1; // ex.: 4 núcleos = 1111 = 0xF
        boolean windows = System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("win");

        List<String> comando = windows
                ? List.of("powershell", "-NoProfile", "-NonInteractive", "-Command",
                "(Get-Process -Id " + pid + ").ProcessorAffinity = " + mascara)
                : List.of("taskset", "-a", "-p", Long.toHexString(mascara), String.valueOf(pid));

        Process p = new ProcessBuilder(comando).redirectErrorStream(true).start();
        String saida = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
        if (p.waitFor() != 0 || (windows && !saida.isEmpty())) {
            throw new IOException("Não foi possível limitar os núcleos: " + saida);
        }
    }
}