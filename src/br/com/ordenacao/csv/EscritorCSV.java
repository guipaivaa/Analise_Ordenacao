package br.com.ordenacao.csv;

import br.com.ordenacao.benchmark.Medicao;
import br.com.ordenacao.benchmark.Resumo;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

/**
 * Grava os CSVs na pasta "resultados".
 * Formato padrão: separador vírgula e decimal com ponto (abre direto no
 * Python/pandas, Google Sheets e LibreOffice; no Excel use Dados > De Texto/CSV).
 */
public class EscritorCSV implements AutoCloseable {

    private static final String SEP = ",";
    private final BufferedWriter brutos;
    private final int nucleos = Runtime.getRuntime().availableProcessors();

    /** Abre o CSV de medições brutas. Cada linha é gravada assim que medida. */
    public EscritorCSV(Path arquivoBrutos) {
        try {
            Files.createDirectories(arquivoBrutos.getParent());
            brutos = Files.newBufferedWriter(arquivoBrutos, StandardCharsets.UTF_8);
            brutos.write(String.join(SEP, "algoritmo", "modo", "threads", "nucleos_disponiveis",
                    "tipo_dados", "tamanho", "valor_maximo", "amostra", "tempo_ms"));
            brutos.newLine();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void escrever(Medicao m) {
        try {
            brutos.write(String.join(SEP, m.algoritmo(), m.modo(), String.valueOf(m.threads()),
                    String.valueOf(nucleos), m.tipo().name(), String.valueOf(m.tamanho()),
                    String.valueOf(m.valorMaximo()), String.valueOf(m.amostra()), num(m.tempoMs())));
            brutos.newLine();
            brutos.flush(); // se o programa cair, o que já foi medido não se perde
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Grava o CSV de resumo (uma linha por cenário, com médias e speedup). */
    public static void escreverResumo(Path arquivo, List<Resumo.Linha> linhas) {
        try (BufferedWriter w = Files.newBufferedWriter(arquivo, StandardCharsets.UTF_8)) {
            w.write(String.join(SEP, "algoritmo", "modo", "threads", "tipo_dados", "tamanho", "valor_maximo",
                    "amostras", "media_ms", "desvio_padrao_ms", "min_ms", "max_ms", "speedup", "eficiencia"));
            w.newLine();
            for (Resumo.Linha l : linhas) {
                w.write(String.join(SEP, l.algoritmo(), l.modo(), String.valueOf(l.threads()), l.tipo().name(),
                        String.valueOf(l.tamanho()), String.valueOf(l.valorMaximo()), String.valueOf(l.amostras()),
                        num(l.mediaMs()), num(l.desvioMs()), num(l.minMs()), num(l.maxMs()),
                        num(l.speedup()), num(l.eficiencia())));
                w.newLine();
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static String num(double x) {
        return Double.isNaN(x) ? "" : String.format(Locale.US, "%.4f", x);
    }

    @Override
    public void close() {
        try {
            brutos.close();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
