package br.com.ordenacao.grafico;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Graphics2D;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Janela de gráficos dinâmicos (ponto extra do trabalho).
 * Lê um ou mais resumo_*.csv e redesenha o gráfico sempre que um filtro muda.
 *
 * Como usar: rode este main. Ele abre sozinho, da pasta "resultados", o resumo mais recente
 * de cada número de núcleos (ex.: as rodadas com 4, 8 e 12 núcleos).
 * Para abrir outros arquivos, use o botão "Abrir CSV..." (dá para selecionar vários com Ctrl).
 */
public class VisualizadorGraficos extends JFrame {

    private final JComboBox<TipoGrafico> cbGrafico = new JComboBox<>(TipoGrafico.values());
    private final JComboBox<String> cbAlgoritmo = new JComboBox<>();
    private final JComboBox<String> cbTipo = new JComboBox<>();
    private final JComboBox<Integer> cbIntervalo = new JComboBox<>();
    private final JComboBox<Integer> cbNucleos = new JComboBox<>();
    private final JCheckBox chkLog = new JCheckBox("Tempo em escala log", true);
    private final GraficoPanel grafico = new GraficoPanel();
    private final JLabel status = new JLabel(" ");

    private List<DadosResumo.Ponto> dados = List.of();
    private String nomeSugerido = "grafico";
    private boolean carregando;

    public VisualizadorGraficos() {
        super("Gráficos — Ordenação Serial vs Paralela");

        JButton abrir = new JButton("Abrir CSV...");
        JButton salvar = new JButton("Salvar PNG");

        JPanel barra1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        barra1.add(abrir);
        barra1.add(new JLabel("   Gráfico:"));
        barra1.add(cbGrafico);
        barra1.add(chkLog);
        barra1.add(salvar);

        JPanel barra2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        barra2.add(new JLabel("Algoritmo:"));
        barra2.add(cbAlgoritmo);
        barra2.add(new JLabel("Dados:"));
        barra2.add(cbTipo);
        barra2.add(new JLabel("Valores até:"));
        barra2.add(cbIntervalo);
        barra2.add(new JLabel("Núcleos:"));
        barra2.add(cbNucleos);

        JPanel topo = new JPanel(new BorderLayout());
        topo.add(barra1, BorderLayout.NORTH);
        topo.add(barra2, BorderLayout.SOUTH);

        status.setBorder(BorderFactory.createEmptyBorder(4, 10, 6, 10));

        setLayout(new BorderLayout());
        add(topo, BorderLayout.NORTH);
        add(grafico, BorderLayout.CENTER);
        add(status, BorderLayout.SOUTH);

        ActionListener atualizar = e -> {
            if (!carregando) atualizar();
        };
        cbGrafico.addActionListener(atualizar);
        cbAlgoritmo.addActionListener(atualizar);
        cbTipo.addActionListener(atualizar);
        cbIntervalo.addActionListener(atualizar);
        cbNucleos.addActionListener(atualizar);
        chkLog.addActionListener(atualizar);
        abrir.addActionListener(e -> escolherArquivos());
        salvar.addActionListener(e -> salvarPng());

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        pack();
        setLocationRelativeTo(null);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignorada) {
                // fica com o visual padrão do Java
            }
            VisualizadorGraficos janela = new VisualizadorGraficos();
            janela.setVisible(true);
            List<Path> iniciais = args.length > 0
                    ? Arrays.stream(args).map(Path::of).toList()
                    : resumosIniciais(Path.of("resultados"));
            if (!iniciais.isEmpty()) janela.carregar(iniciais);
            else janela.escolherArquivos();
        });
    }

    /**
     * Escolhe os arquivos abertos automaticamente: o resumo mais recente de cada número de núcleos
     * (dando preferência ao perfil "completo"). Se nenhum arquivo indicar núcleos no nome,
     * abre só o resumo mais recente.
     */
    static List<Path> resumosIniciais(Path pasta) {
        if (!Files.isDirectory(pasta)) return List.of();
        List<Path> resumos;
        try (Stream<Path> arquivos = Files.list(pasta)) {
            resumos = arquivos.filter(p -> p.getFileName().toString().startsWith("resumo_")
                    && p.getFileName().toString().endsWith(".csv")).toList();
        } catch (IOException e) {
            return List.of();
        }
        Comparator<Path> porData = Comparator.comparingLong(p -> p.toFile().lastModified());

        List<Path> comNucleos = resumos.stream().filter(p -> DadosResumo.nucleosDoNome(p) > 0).toList();
        if (comNucleos.isEmpty()) {
            return resumos.stream().max(porData).map(List::of).orElse(List.of());
        }
        List<Path> completos = comNucleos.stream()
                .filter(p -> p.getFileName().toString().startsWith("resumo_completo_")).toList();
        List<Path> candidatos = completos.isEmpty() ? comNucleos : completos;

        Map<Integer, Path> maisRecentePorNucleos = new TreeMap<>();
        for (Path p : candidatos) {
            maisRecentePorNucleos.merge(DadosResumo.nucleosDoNome(p), p,
                    (a, b) -> porData.compare(a, b) >= 0 ? a : b);
        }
        return new ArrayList<>(maisRecentePorNucleos.values());
    }

    private void escolherArquivos() {
        File pasta = new File("resultados");
        JFileChooser fc = new JFileChooser(pasta.isDirectory() ? pasta : new File("."));
        fc.setDialogTitle("Escolha um ou mais arquivos resumo_*.csv (Ctrl para selecionar vários)");
        fc.setFileFilter(new FileNameExtensionFilter("Arquivos CSV", "csv"));
        fc.setMultiSelectionEnabled(true);
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            carregar(Arrays.stream(fc.getSelectedFiles()).map(File::toPath).toList());
        }
    }

    private void carregar(List<Path> arquivos) {
        try {
            dados = DadosResumo.lerVarios(arquivos);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Não foi possível ler o arquivo:\n" + e.getMessage(),
                    "Erro", JOptionPane.ERROR_MESSAGE);
            return;
        }
        carregando = true;
        preencher(cbAlgoritmo, dados.stream().map(DadosResumo.Ponto::algoritmo).distinct().toList());
        preencher(cbTipo, dados.stream().map(DadosResumo.Ponto::tipo).distinct().toList());
        preencher(cbIntervalo, dados.stream().map(DadosResumo.Ponto::valorMaximo).distinct().sorted().toList());
        List<Integer> nucleos = dados.stream().map(DadosResumo.Ponto::nucleos).distinct().sorted().toList();
        preencher(cbNucleos, nucleos);
        if (cbNucleos.getSelectedItem() == null && !nucleos.isEmpty()) {
            cbNucleos.setSelectedItem(nucleos.get(nucleos.size() - 1));
        }
        carregando = false;

        String nomes = arquivos.stream().map(p -> p.getFileName().toString()).collect(Collectors.joining(", "));
        status.setText("<html>Arquivos: " + nomes + "<br>" + dados.size()
                + " cenários  ·  Passe o mouse sobre os pontos para ver os valores exatos.</html>");
        atualizar();
    }

    /** Troca os itens do combo mantendo a seleção atual, se ela ainda existir. */
    private static <T> void preencher(JComboBox<T> combo, List<T> itens) {
        Object atual = combo.getSelectedItem();
        combo.removeAllItems();
        itens.forEach(combo::addItem);
        if (atual != null && itens.contains(atual)) combo.setSelectedItem(atual);
    }

    private void atualizar() {
        TipoGrafico tipo = (TipoGrafico) cbGrafico.getSelectedItem();
        String algoritmo = (String) cbAlgoritmo.getSelectedItem();
        String tipoDados = (String) cbTipo.getSelectedItem();
        Integer intervalo = (Integer) cbIntervalo.getSelectedItem();
        Integer nucleos = (Integer) cbNucleos.getSelectedItem();
        if (tipo == null || algoritmo == null || tipoDados == null || intervalo == null || nucleos == null) return;

        cbAlgoritmo.setEnabled(tipo != TipoGrafico.COMPARAR && tipo != TipoGrafico.MELHOR_NUCLEOS);
        cbNucleos.setEnabled(!tipo.comparaNucleos());
        chkLog.setEnabled(tipo == TipoGrafico.TEMPO_TAMANHO || tipo == TipoGrafico.TEMPO_THREADS
                || tipo == TipoGrafico.COMPARAR);

        nomeSugerido = MontadorGraficos.montar(grafico, Objects.requireNonNull(tipo), dados,
                algoritmo, tipoDados, intervalo, nucleos, chkLog.isSelected());
    }

    private void salvarPng() {
        if (dados.isEmpty()) return;
        File pasta = new File("resultados", "graficos");
        pasta.mkdirs();
        JFileChooser fc = new JFileChooser(pasta);
        fc.setSelectedFile(new File(pasta, nomeSugerido + ".png"));
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;

        File destino = fc.getSelectedFile();
        if (!destino.getName().toLowerCase().endsWith(".png")) {
            destino = new File(destino.getParentFile(), destino.getName() + ".png");
        }
        try {
            ImageIO.write(renderizar(grafico, grafico.getWidth(), grafico.getHeight()), "png", destino);
            status.setText("Imagem salva em: " + destino.getAbsolutePath());
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Erro ao salvar: " + e.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    /** Desenha o painel numa imagem em resolução dobrada (fica nítida no relatório). */
    static BufferedImage renderizar(GraficoPanel painel, int largura, int altura) {
        painel.setSize(largura, altura);
        BufferedImage img = new BufferedImage(largura * 2, altura * 2, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.scale(2, 2);
        painel.paint(g);
        g.dispose();
        return img;
    }
}