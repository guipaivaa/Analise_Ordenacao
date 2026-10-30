package br.com.ordenacao.grafico;

import javax.swing.JPanel;
import javax.swing.ToolTipManager;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.TreeSet;

/**
 * Componente Swing que desenha um gráfico de linhas, em Java puro (sem bibliotecas).
 * Suporta eixo X numérico (linear ou log) ou por categorias, eixo Y linear ou log,
 * linha de referência, legenda e tooltip com o valor exato ao passar o mouse.
 */
public class GraficoPanel extends JPanel {

    /**
     * Uma série de pontos (x, y). Em eixo por categorias, x é o índice da categoria.
     * cor = índice fixo na paleta, ou -1 para usar a próxima cor automaticamente.
     */
    public record Serie(String nome, List<double[]> pontos, boolean tracejada, int cor) {
        public Serie(String nome, List<double[]> pontos, boolean tracejada) {
            this(nome, pontos, tracejada, -1);
        }
    }

    private static final Color[] CORES = {
            new Color(0x1F77B4), new Color(0xD62728), new Color(0x2CA02C), new Color(0xFF7F0E),
            new Color(0x9467BD), new Color(0x8C564B), new Color(0xE377C2), new Color(0x17BECF),
            new Color(0x7F7F7F), new Color(0xBCBD22)};

    private static final Locale BR = Locale.forLanguageTag("pt-BR");

    private String titulo = "";
    private String subtitulo = "";
    private String rotuloX = "";
    private String rotuloY = "";
    private String unidadeY = "";
    private List<Serie> series = List.of();
    private List<String> categorias;   // null = eixo X numérico
    private boolean xLog;
    private boolean yLog;
    private Double referencia;
    private String rotuloReferencia;

    private record PontoTela(int x, int y, String texto) {
    }

    private final List<PontoTela> pontosTela = new ArrayList<>();

    public GraficoPanel() {
        setPreferredSize(new Dimension(1150, 640));
        setBackground(Color.WHITE);
        ToolTipManager.sharedInstance().registerComponent(this);
        ToolTipManager.sharedInstance().setInitialDelay(100);
    }

    public void mostrar(String titulo, String subtitulo, String rotuloX, String rotuloY, String unidadeY,
                        List<Serie> series, List<String> categorias, boolean xLog, boolean yLog,
                        Double referencia, String rotuloReferencia) {
        this.titulo = titulo;
        this.subtitulo = subtitulo;
        this.rotuloX = rotuloX;
        this.rotuloY = rotuloY;
        this.unidadeY = unidadeY;
        this.series = series;
        this.categorias = categorias;
        this.xLog = xLog && categorias == null;
        this.yLog = yLog;
        this.referencia = referencia;
        this.rotuloReferencia = rotuloReferencia;
        repaint();
    }

    // ---------------------------------------------------------------- desenho

    @Override
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        int w = getWidth(), h = getHeight();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, w, h);
        pontosTela.clear();

        Font base = new Font(Font.SANS_SERIF, Font.PLAIN, 12);
        g.setFont(base.deriveFont(Font.BOLD, 16f));
        g.setColor(new Color(0x222222));
        centralizar(g, titulo, w / 2, 26);
        g.setFont(base.deriveFont(13f));
        g.setColor(new Color(0x555555));
        centralizar(g, subtitulo, w / 2, 45);
        g.setFont(base);
        FontMetrics fm = g.getFontMetrics();

        List<Serie> visiveis = series.stream().filter(s -> s.pontos().stream().anyMatch(this::valido)).toList();
        if (visiveis.isEmpty()) {
            centralizar(g, "Sem dados para esta combinação de filtros", w / 2, h / 2);
            g.dispose();
            return;
        }

        int larguraLegenda = 0;
        for (Serie s : visiveis) larguraLegenda = Math.max(larguraLegenda, fm.stringWidth(s.nome()));
        if (referencia != null) larguraLegenda = Math.max(larguraLegenda, fm.stringWidth(rotuloReferencia));
        larguraLegenda += 45;

        int esq = 90, dir = larguraLegenda + 25, topo = 65, baixo = 60;
        int pw = w - esq - dir, ph = h - topo - baixo;
        if (pw < 80 || ph < 80) {
            g.dispose();
            return;
        }

        // ---- faixas dos eixos (em coordenadas transformadas: log10 quando log)
        double minX = Double.MAX_VALUE, maxX = -Double.MAX_VALUE, minY = Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
        for (Serie s : visiveis) {
            for (double[] p : s.pontos()) {
                if (!valido(p)) continue;
                minX = Math.min(minX, p[0]);
                maxX = Math.max(maxX, p[0]);
                minY = Math.min(minY, p[1]);
                maxY = Math.max(maxY, p[1]);
            }
        }
        if (referencia != null) {
            minY = Math.min(minY, referencia);
            maxY = Math.max(maxY, referencia);
        }

        final double x0, x1, y0, y1;
        if (categorias != null) {
            x0 = -0.5;
            x1 = categorias.size() - 0.5;
        } else if (xLog) {
            x0 = Math.log10(minX) - 0.2;
            x1 = Math.log10(maxX) + 0.2;
        } else {
            double pad = Math.max((maxX - minX) * 0.05, 0.5);
            x0 = minX - pad;
            x1 = maxX + pad;
        }
        double passoY = 0;
        if (yLog) {
            double a = Math.floor(Math.log10(minY));
            double b = Math.ceil(Math.log10(maxY));
            y0 = a;
            y1 = (b <= a) ? a + 1 : b;
        } else {
            y0 = 0;
            passoY = passoBonito(maxY * 1.08 / 6);
            y1 = Math.ceil(maxY * 1.08 / passoY) * passoY;
        }

        // conversões valor → pixel
        java.util.function.DoubleUnaryOperator px = v -> esq + (tx(v) - x0) / (x1 - x0) * pw;
        java.util.function.DoubleUnaryOperator py = v -> topo + ph - (ty(v) - y0) / (y1 - y0) * ph;

        Color grade = new Color(0xE6E6E6);
        Color textoEixo = new Color(0x444444);

        // ---- grade e rótulos do eixo Y
        List<Double> ticksY = new ArrayList<>();
        List<Boolean> rotuladoY = new ArrayList<>();
        if (yLog) {
            boolean poucasDecadas = (y1 - y0) <= 3;
            for (int k = (int) y0; k < (int) y1; k++) {
                for (int m : new int[]{1, 2, 5}) {
                    ticksY.add(m * Math.pow(10, k));
                    rotuladoY.add(m == 1 || poucasDecadas);
                }
            }
            ticksY.add(Math.pow(10, y1));
            rotuladoY.add(true);
        } else {
            for (double v = 0; v <= y1 + passoY * 1e-6; v += passoY) {
                ticksY.add(v);
                rotuladoY.add(true);
            }
        }
        for (int i = 0; i < ticksY.size(); i++) {
            int y = (int) Math.round(py.applyAsDouble(ticksY.get(i)));
            g.setColor(grade);
            g.drawLine(esq, y, esq + pw, y);
            if (rotuladoY.get(i)) {
                g.setColor(textoEixo);
                String t = formatar(ticksY.get(i));
                g.drawString(t, esq - 8 - fm.stringWidth(t), y + fm.getAscent() / 2 - 1);
            }
        }

        // ---- grade e rótulos do eixo X
        if (categorias != null) {
            for (int i = 0; i < categorias.size(); i++) {
                int x = (int) Math.round(px.applyAsDouble(i));
                g.setColor(grade);
                g.drawLine(x, topo, x, topo + ph);
                g.setColor(textoEixo);
                centralizar(g, categorias.get(i), x, topo + ph + 18);
            }
        } else {
            TreeSet<Double> valoresX = new TreeSet<>();
            for (Serie s : visiveis) for (double[] p : s.pontos()) if (valido(p)) valoresX.add(p[0]);
            for (double v : valoresX) {
                int x = (int) Math.round(px.applyAsDouble(v));
                g.setColor(grade);
                g.drawLine(x, topo, x, topo + ph);
                g.setColor(textoEixo);
                centralizar(g, formatarTamanho(v), x, topo + ph + 18);
            }
        }

        // ---- eixos
        g.setColor(new Color(0x888888));
        g.drawLine(esq, topo + ph, esq + pw, topo + ph);
        g.drawLine(esq, topo, esq, topo + ph);

        // ---- títulos dos eixos
        g.setFont(base.deriveFont(Font.BOLD, 13f));
        g.setColor(new Color(0x333333));
        centralizar(g, rotuloX + (xLog ? "  (escala log)" : ""), esq + pw / 2, h - 15);
        AffineTransform original = g.getTransform();
        g.rotate(-Math.PI / 2);
        centralizar(g, rotuloY + (yLog ? "  (escala log)" : ""), -(topo + ph / 2), 22);
        g.setTransform(original);
        g.setFont(base);

        // ---- linha de referência
        Stroke tracejado = new BasicStroke(1.6f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[]{7f, 5f}, 0f);
        if (referencia != null) {
            int y = (int) Math.round(py.applyAsDouble(referencia));
            g.setColor(new Color(0x666666));
            g.setStroke(tracejado);
            g.drawLine(esq, y, esq + pw, y);
        }

        // ---- séries
        for (int i = 0; i < visiveis.size(); i++) {
            Serie s = visiveis.get(i);
            Color cor = corDa(s, i);
            List<double[]> pts = s.pontos().stream().filter(this::valido)
                    .sorted(Comparator.comparingDouble(p -> p[0])).toList();

            Path2D linha = new Path2D.Double();
            for (int k = 0; k < pts.size(); k++) {
                double x = px.applyAsDouble(pts.get(k)[0]), y = py.applyAsDouble(pts.get(k)[1]);
                if (k == 0) linha.moveTo(x, y);
                else linha.lineTo(x, y);
            }
            g.setColor(cor);
            g.setStroke(s.tracejada()
                    ? new BasicStroke(2.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 10f, new float[]{8f, 5f}, 0f)
                    : new BasicStroke(2.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.draw(linha);
            g.setStroke(new BasicStroke(1.5f));
            for (double[] p : pts) {
                int x = (int) Math.round(px.applyAsDouble(p[0])), y = (int) Math.round(py.applyAsDouble(p[1]));
                g.setColor(Color.WHITE);
                g.fillOval(x - 4, y - 4, 8, 8);
                g.setColor(cor);
                g.drawOval(x - 4, y - 4, 8, 8);
                String vx = categorias != null ? categorias.get((int) p[0]) : formatarTamanho(p[0]);
                pontosTela.add(new PontoTela(x, y, "<html><b>" + s.nome() + "</b><br>" + rotuloX + ": " + vx
                        + "<br>" + rotuloY + ": " + formatar(p[1]) + unidadeY + "</html>"));
            }
        }

        // ---- legenda
        int lx = esq + pw + 20, ly = topo + 8;
        g.setColor(new Color(0xF7F7F7));
        int alturaLegenda = (visiveis.size() + (referencia != null ? 1 : 0)) * 20 + 10;
        g.fillRoundRect(lx - 8, ly - 8, larguraLegenda, alturaLegenda, 8, 8);
        for (int i = 0; i < visiveis.size(); i++) {
            Serie s = visiveis.get(i);
            int y = ly + i * 20 + 6;
            g.setColor(corDa(s, i));
            g.setStroke(s.tracejada()
                    ? new BasicStroke(2.4f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[]{5f, 3f}, 0f)
                    : new BasicStroke(2.4f));
            g.drawLine(lx, y, lx + 24, y);
            g.setColor(new Color(0x333333));
            g.drawString(s.nome(), lx + 32, y + fm.getAscent() / 2 - 1);
        }
        if (referencia != null) {
            int y = ly + visiveis.size() * 20 + 6;
            g.setColor(new Color(0x666666));
            g.setStroke(tracejado);
            g.drawLine(lx, y, lx + 24, y);
            g.setColor(new Color(0x333333));
            g.drawString(rotuloReferencia, lx + 32, y + fm.getAscent() / 2 - 1);
        }
        g.dispose();
    }

    @Override
    public String getToolTipText(MouseEvent e) {
        PontoTela melhor = null;
        double melhorDist = 10 * 10;
        for (PontoTela p : pontosTela) {
            double d = Math.pow(p.x() - e.getX(), 2) + Math.pow(p.y() - e.getY(), 2);
            if (d < melhorDist) {
                melhorDist = d;
                melhor = p;
            }
        }
        return melhor == null ? null : melhor.texto();
    }

    // ---------------------------------------------------------------- auxiliares

    private static Color corDa(Serie s, int i) {
        return CORES[(s.cor() >= 0 ? s.cor() : i) % CORES.length];
    }

    private boolean valido(double[] p) {
        if (Double.isNaN(p[0]) || Double.isNaN(p[1]) || Double.isInfinite(p[1])) return false;
        if (yLog && p[1] <= 0) return false;
        return !xLog || p[0] > 0;
    }

    private double tx(double v) {
        return xLog ? Math.log10(v) : v;
    }

    private double ty(double v) {
        return yLog ? Math.log10(v) : v;
    }

    private static void centralizar(Graphics2D g, String t, int x, int y) {
        if (t == null || t.isEmpty()) return;
        g.drawString(t, x - g.getFontMetrics().stringWidth(t) / 2, y);
    }

    /** Passo "redondo" (1, 2, 2,5 ou 5 × 10^k) próximo do valor bruto. */
    private static double passoBonito(double bruto) {
        if (bruto <= 0) return 1;
        double exp = Math.pow(10, Math.floor(Math.log10(bruto)));
        double f = bruto / exp;
        double n = f <= 1 ? 1 : f <= 2 ? 2 : f <= 2.5 ? 2.5 : f <= 5 ? 5 : 10;
        return n * exp;
    }

    public static String formatar(double v) {
        NumberFormat nf = NumberFormat.getNumberInstance(BR);
        double a = Math.abs(v);
        nf.setMaximumFractionDigits(a >= 100 ? 0 : a >= 1 ? 2 : a >= 0.01 ? 3 : 4);
        return nf.format(v);
    }

    public static String formatarTamanho(double v) {
        if (v >= 1_000_000 && v % 1_000_000 == 0) return (long) (v / 1_000_000) + "M";
        if (v >= 1_000 && v % 1_000 == 0) return (long) (v / 1_000) + "k";
        return formatar(v);
    }
}