package br.com.ordenacao.grafico;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Lê o arquivo resumo_*.csv gerado pelo benchmark. */
public final class DadosResumo {

    private DadosResumo() {
    }

    /**
     * Uma linha do resumo: um cenário (algoritmo × modo × threads × tipo × tamanho × intervalo).
     * 'nucleos' vem do nome do arquivo (ex.: resumo_completo_4nucleos_...csv); 0 se não houver.
     */
    public record Ponto(int nucleos, String algoritmo, String modo, int threads, String tipo, int tamanho,
                        int valorMaximo, double mediaMs, double desvioMs, double speedup, double eficiencia) {

        public boolean serial() {
            return modo.equals("SERIAL");
        }

        public String nomeSerie() {
            return serial() ? "Serial" : "Paralelo " + threads + (threads == 1 ? " thread" : " threads");
        }
    }

    private static final String[] COLUNAS = {"algoritmo", "modo", "threads", "tipo_dados", "tamanho",
            "valor_maximo", "media_ms", "desvio_padrao_ms", "speedup", "eficiencia"};

    private static final Pattern NUCLEOS_NO_NOME = Pattern.compile("_(\\d+)nucleos_");

    /** Número de núcleos indicado no nome do arquivo, ou 0 se o nome não indicar. */
    public static int nucleosDoNome(Path arquivo) {
        Matcher m = NUCLEOS_NO_NOME.matcher(arquivo.getFileName().toString());
        return m.find() ? Integer.parseInt(m.group(1)) : 0;
    }

    /** Lê vários resumos (ex.: as rodadas com 4, 8 e 12 núcleos) numa lista só. */
    public static List<Ponto> lerVarios(List<Path> arquivos) throws IOException {
        List<Ponto> todos = new ArrayList<>();
        for (Path a : arquivos) {
            todos.addAll(ler(a));
        }
        return todos;
    }

    public static List<Ponto> ler(Path arquivo) throws IOException {
        int nucleos = nucleosDoNome(arquivo);
        List<String> linhas = Files.readAllLines(arquivo, StandardCharsets.UTF_8);
        if (linhas.isEmpty()) {
            throw new IllegalArgumentException("Arquivo vazio.");
        }

        String cabecalho = linhas.get(0).replace("\uFEFF", "");
        // Se o CSV foi salvo de novo pelo Excel em português, o separador vira ';' e o decimal vira ','.
        boolean formatoExcelBr = cabecalho.contains(";");
        String sep = formatoExcelBr ? ";" : ",";

        String[] nomes = cabecalho.split(sep);
        Map<String, Integer> idx = new HashMap<>();
        for (int i = 0; i < nomes.length; i++) {
            idx.put(nomes[i].trim().toLowerCase(), i);
        }
        for (String c : COLUNAS) {
            if (!idx.containsKey(c)) {
                throw new IllegalArgumentException("Este não parece um arquivo resumo_*.csv (falta a coluna \"" + c + "\").");
            }
        }

        List<Ponto> pontos = new ArrayList<>();
        for (int n = 1; n < linhas.size(); n++) {
            String linha = linhas.get(n);
            if (linha.isBlank()) continue;
            String[] c = linha.split(sep, -1);
            pontos.add(new Ponto(
                    nucleos,
                    c[idx.get("algoritmo")].trim(),
                    c[idx.get("modo")].trim(),
                    Integer.parseInt(c[idx.get("threads")].trim()),
                    c[idx.get("tipo_dados")].trim(),
                    Integer.parseInt(c[idx.get("tamanho")].trim()),
                    Integer.parseInt(c[idx.get("valor_maximo")].trim()),
                    num(c[idx.get("media_ms")], formatoExcelBr),
                    num(c[idx.get("desvio_padrao_ms")], formatoExcelBr),
                    num(c[idx.get("speedup")], formatoExcelBr),
                    num(c[idx.get("eficiencia")], formatoExcelBr)));
        }
        return pontos;
    }

    private static double num(String s, boolean decimalVirgula) {
        s = s.trim();
        if (s.isEmpty()) return Double.NaN;
        if (decimalVirgula) s = s.replace(".", "").replace(',', '.');
        return Double.parseDouble(s);
    }
}