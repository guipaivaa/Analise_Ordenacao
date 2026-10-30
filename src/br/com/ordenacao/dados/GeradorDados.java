package br.com.ordenacao.dados;

import java.util.Random;

/**
 * Gera os vetores de entrada. Usa semente fixa para que serial e paralelo
 * (e você e sua dupla) ordenem exatamente os mesmos dados.
 */
public final class GeradorDados {

    private GeradorDados() {
    }

    /**
     * @param tamanho     quantidade de elementos
     * @param tipo        natureza dos dados
     * @param valorMaximo valores ficam em [0, valorMaximo) — o "intervalo de valores"
     * @param semente     semente do gerador aleatório
     */
    public static int[] gerar(int tamanho, TipoDados tipo, int valorMaximo, long semente) {
        Random rnd = new Random(semente);
        int[] v = new int[tamanho];

        switch (tipo) {
            case ALEATORIO -> {
                for (int i = 0; i < tamanho; i++) v[i] = rnd.nextInt(valorMaximo);
            }
            case ORDENADO -> preencherCrescente(v, valorMaximo);
            case INVERSO -> {
                preencherCrescente(v, valorMaximo);
                inverter(v);
            }
            case QUASE_ORDENADO -> {
                preencherCrescente(v, valorMaximo);
                int trocas = Math.max(1, tamanho / 20); // ~5%
                for (int k = 0; k < trocas; k++) {
                    int a = rnd.nextInt(tamanho), b = rnd.nextInt(tamanho);
                    int tmp = v[a]; v[a] = v[b]; v[b] = tmp;
                }
            }
        }
        return v;
    }

    /** Valores crescentes espalhados por todo o intervalo [0, valorMaximo). */
    private static void preencherCrescente(int[] v, int valorMaximo) {
        for (int i = 0; i < v.length; i++) {
            v[i] = (int) ((long) i * valorMaximo / v.length);
        }
    }

    private static void inverter(int[] v) {
        for (int i = 0, j = v.length - 1; i < j; i++, j--) {
            int tmp = v[i]; v[i] = v[j]; v[j] = tmp;
        }
    }
}
