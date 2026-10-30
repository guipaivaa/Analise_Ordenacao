Análise de Desempenho de Algoritmos de Ordenação: Serial vs Paralelo em Java
Estudo comparativo do desempenho de quatro algoritmos de ordenação — Bubble Sort, Quick Sort, Merge Sort e Counting Sort — em versões sequenciais e paralelas, variando o tamanho e a natureza dos dados, o intervalo de valores, o número de threads e o número de núcleos de processamento disponíveis.
O projeto inclui:
as 8 implementações (4 sequenciais e 4 paralelas);
um framework de testes que mede, valida e registra cada execução em arquivos CSV;
uma ferramenta de gráficos dinâmicos em Java Swing para explorar os resultados;
os resultados completos das execuções com 4, 8 e 12 núcleos.
---
Sumário
Algoritmos e estratégias de paralelização
Estrutura do projeto
Requisitos
Como abrir no IntelliJ IDEA
Como executar o benchmark
Como limitar o número de núcleos
Como executar pela linha de comando
Arquivos de resultado (CSV)
Ferramenta de gráficos
Principais resultados
Como adicionar um novo algoritmo
---
1. Algoritmos e estratégias de paralelização
   Algoritmo	Complexidade (caso médio)	Estratégia paralela	Principal limitação
   Bubble Sort	O(n²)	Transposição ímpar-par: fases pares e ímpares divididas entre as threads, com `CyclicBarrier` entre as fases	Até n sincronizações entre todas as threads
   Quick Sort	O(n log n)	`ForkJoinPool`: os dois lados de cada partição viram tarefas independentes	A primeira partição é feita por uma só thread
   Merge Sort	O(n log n)	`ForkJoinPool`: as duas metades são ordenadas em paralelo e depois intercaladas	A intercalação final é feita por uma só thread
   Counting Sort	O(n + k)	Cada thread conta seu bloco num vetor próprio; as contagens são somadas e cada thread escreve uma faixa de valores	Cada thread aloca um vetor de tamanho k (intervalo de valores)
   Detalhes de implementação:
   Quick Sort usa partição de Hoare com pivô pela mediana de três, evitando o pior caso O(n²) em vetores ordenados ou invertidos.
   Quick Sort e Merge Sort paralelos ordenam de forma sequencial os subvetores com menos de `max(8192, n / (8 × threads))` elementos, para que o custo de criar tarefas não supere o trabalho útil.
   Merge Sort pula a intercalação quando as duas metades já estão em ordem, o que o torna muito rápido com dados ordenados.
   As versões paralelas reaproveitam o código das sequenciais (partição, intercalação), então as duas versões de cada algoritmo fazem o mesmo trabalho.
---
2. Estrutura do projeto
```
Analise_Ordenacao/
├── .idea/                          configuração do IntelliJ
├── AnaliseOrdenacao.iml            módulo do IntelliJ
├── docs/                           imagens do README
├── resultados/                     CSVs gerados (os das execuções finais já estão aqui)
└── src/br/com/ordenacao/
    ├── Main.java                   ponto de entrada: escolhe o perfil e o nº de núcleos
    ├── LimitadorNucleos.java       restringe o processo a N núcleos (afinidade de processador)
    │
    ├── algoritmos/
    │   ├── Ordenador.java          interface comum a todos os algoritmos
    │   ├── serial/                 BubbleSortSerial, QuickSortSerial, MergeSortSerial, CountingSortSerial
    │   └── paralelo/               BubbleSortParalelo, QuickSortParalelo, MergeSortParalelo, CountingSortParalelo
    │
    ├── dados/
    │   ├── TipoDados.java          ALEATORIO, ORDENADO, INVERSO, QUASE_ORDENADO
    │   └── GeradorDados.java       gera os vetores de entrada com semente fixa
    │
    ├── benchmark/
    │   ├── Algoritmo.java          fábrica que liga cada algoritmo às suas duas versões
    │   ├── ConfigBenchmark.java    parâmetros de um experimento (tamanhos, tipos, threads...)
    │   ├── Benchmark.java          executa, mede e valida cada cenário
    │   ├── Medicao.java            uma medição individual
    │   └── Resumo.java             média, desvio padrão, speedup e eficiência
    │
    ├── csv/
    │   └── EscritorCSV.java        grava os arquivos de resultado
    │
    └── grafico/
        ├── VisualizadorGraficos.java   janela Swing (ponto de entrada dos gráficos)
        ├── GraficoPanel.java           componente que desenha os gráficos (Java puro)
        ├── MontadorGraficos.java       monta cada tipo de gráfico a partir dos dados
        ├── DadosResumo.java            lê os arquivos resumo_*.csv
        └── TipoGrafico.java            tipos de gráfico disponíveis
```
O projeto não usa nenhuma biblioteca externa: tudo é Java padrão, inclusive os gráficos.
---
3. Requisitos
   JDK 17 ou superior (os experimentos foram feitos com o JDK 21).
   IntelliJ IDEA (Community ou Ultimate) — opcional, também é possível rodar pela linha de comando.
   Windows ou Linux. A limitação de núcleos usa o PowerShell no Windows e o comando `taskset` no Linux.
---
4. Como abrir no IntelliJ IDEA
   Clone o repositório:
```bash
   git clone https://github.com/guipaivaa/Analise_Ordenacao.git
   ```
ou baixe pelo botão Code > Download ZIP e descompacte.
No IntelliJ: File > Open e selecione a pasta do projeto.
Se aparecer o aviso "Project JDK is not defined", clique em Setup SDK e escolha um JDK 17 ou superior. Se não tiver nenhum instalado, a própria janela oferece Download JDK.
---
5. Como executar o benchmark
   Abra `src/br/com/ordenacao/Main.java` e clique no ▶ ao lado de `public static void main`.
   Para escolher o perfil, vá em Run > Edit Configurations... e preencha o campo Program arguments:
   Argumento	Duração aproximada	Para que serve
   `teste`	segundos	Verificar se tudo compila e funciona
   `rapido`	poucos minutos	Conferir resultados durante o desenvolvimento (padrão, se nada for informado)
   `completo`	~10 a 15 minutos	Execução final, usada no relatório
   Configuração do perfil `completo`:
   Parâmetro	Valores
   Tamanhos	Bubble Sort: 1 mil a 20 mil · demais: 10 mil a 10 milhões
   Natureza dos dados	aleatório, ordenado, inverso, quase ordenado (5 % das posições trocadas)
   Intervalo de valores	`[0, 1.000)` e `[0, 1.000.000)`
   Threads	1, 2, 4, 8, p e 2p (p = núcleos disponíveis)
   Repetições	2 aquecimentos descartados + 5 amostras medidas por cenário
   Durante a execução, o console mostra uma linha por cenário com o tempo médio. No final, ele exibe o caminho dos dois CSVs gerados.
   Como cada cenário é medido:
   O vetor de entrada é gerado uma única vez, com semente fixa (os mesmos dados para serial e paralelo).
   São feitas 2 execuções de aquecimento, descartadas, para o compilador JIT da JVM otimizar o código.
   São feitas 5 amostras. Em cada uma: o vetor é copiado fora do cronômetro, só a ordenação é medida com `System.nanoTime`, e o resultado é comparado com `Arrays.sort` (também fora do cronômetro). Se alguma ordenação estiver errada, o programa para com erro.
> **Dica:** para medições confiáveis, deixe o notebook na tomada e feche outros programas durante a execução.
---
6. Como limitar o número de núcleos
   Coloque o número de núcleos como segundo argumento:
   Program arguments	Efeito
   `completo 4`	executa o perfil completo usando só 4 núcleos
   `completo 8`	executa o perfil completo usando só 8 núcleos
   `completo 12`	executa o perfil completo usando 12 núcleos
   `completo`	usa todos os núcleos da máquina
   O console deve começar com:
```
Benchmark fixado nos núcleos 0 a 3 de 12.

Perfil: completo | Núcleos disponíveis: 4 | Java 21...
```
Por que não basta usar `-XX:ActiveProcessorCount`? Essa opção só muda o número de núcleos que o Java informa; o sistema operacional continua usando todos. Por isso, o programa se reinicia como um processo filho e o sistema operacional fixa esse processo nos N primeiros núcleos (afinidade de processador). O filho também recebe `-XX:ActiveProcessorCount=N`, para que as threads testadas e o CSV correspondam aos núcleos disponíveis. Se você clicar em Stop no IntelliJ, o processo filho é encerrado junto.
Os núcleos considerados são núcleos lógicos, como o sistema operacional os vê.
---
7. Como executar pela linha de comando (sem IntelliJ)
   Na pasta raiz do projeto:
   Windows (PowerShell):
```powershell
# compilar
javac -encoding UTF-8 -d out (Get-ChildItem -Recurse -Filter *.java src).FullName

# executar o benchmark (perfil completo, 4 núcleos)
java -cp out br.com.ordenacao.Main completo 4

# abrir a ferramenta de gráficos
java -cp out br.com.ordenacao.grafico.VisualizadorGraficos
```
Linux / macOS:
```bash
# compilar
javac -encoding UTF-8 -d out $(find src -name "*.java")

# executar o benchmark (perfil completo, 4 núcleos)
java -cp out br.com.ordenacao.Main completo 4

# abrir a ferramenta de gráficos
java -cp out br.com.ordenacao.grafico.VisualizadorGraficos
```
> A limitação de núcleos funciona no Windows e no Linux. No macOS, rode sem o segundo argumento.
---
8. Arquivos de resultado (CSV)
   Cada execução cria dois arquivos na pasta `resultados/`, com data e hora no nome (nada é sobrescrito):
```
resultados/medicoes_<perfil>_<N>nucleos_<data>_<hora>.csv
resultados/resumo_<perfil>_<N>nucleos_<data>_<hora>.csv
```
O formato é CSV padrão (separador vírgula, decimal com ponto), que abre direto no Python/pandas, Google Sheets e LibreOffice. No Excel, use Dados > De Texto/CSV.
`medicoes_*.csv` — uma linha por amostra
Coluna	Descrição
`algoritmo`	BubbleSort, QuickSort, MergeSort ou CountingSort
`modo`	SERIAL ou PARALELO
`threads`	número de threads (1 no serial)
`nucleos_disponiveis`	núcleos disponíveis na execução
`tipo_dados`	ALEATORIO, ORDENADO, INVERSO ou QUASE_ORDENADO
`tamanho`	número de elementos do vetor
`valor_maximo`	os valores ficam no intervalo `[0, valor_maximo)`
`amostra`	número da amostra (1 a 5)
`tempo_ms`	tempo da ordenação, em milissegundos
`resumo_*.csv` — uma linha por cenário
Coluna	Descrição
`algoritmo`, `modo`, `threads`, `tipo_dados`, `tamanho`, `valor_maximo`	identificam o cenário
`amostras`	quantidade de amostras (5)
`media_ms`	tempo médio
`desvio_padrao_ms`	desvio padrão amostral
`min_ms`, `max_ms`	menor e maior tempo
`speedup`	tempo médio serial ÷ tempo médio paralelo (mesmo cenário e mesma execução)
`eficiencia`	speedup ÷ número de threads (1,0 = aproveitamento ideal)
Resultados incluídos no repositório
Execução	Medições	Resumo
4 núcleos	`medicoes_completo_4nucleos_20261030_145410.csv`	`resumo_completo_4nucleos_20261030_145410.csv`
8 núcleos	`medicoes_completo_8nucleos_20261030_150839.csv`	`resumo_completo_8nucleos_20261030_150839.csv`
12 núcleos	`medicoes_completo_12nucleos_20261030_152043.csv`	`resumo_completo_12nucleos_20261030_152043.csv`
No total: 2.736 cenários e 13.680 medições.
---
9. Ferramenta de gráficos
   Os gráficos são gerados a partir dos CSVs — não é preciso rodar o benchmark de novo.
   Abra `src/br/com/ordenacao/grafico/VisualizadorGraficos.java` e clique no ▶ ao lado do `main`.
   A janela abre automaticamente, da pasta `resultados/`, o resumo mais recente de cada número de núcleos (dando preferência ao perfil `completo`). Para escolher outros arquivos, use Abrir CSV... (dá para selecionar vários com `Ctrl`).
   Filtros (o gráfico é redesenhado na hora a cada mudança):
   Filtro	Opções
   Gráfico	tipo de gráfico (tabela abaixo)
   Algoritmo	BubbleSort, QuickSort, MergeSort, CountingSort
   Dados	natureza dos dados
   Valores até	intervalo de valores (1.000 ou 1.000.000)
   Núcleos	de qual execução vêm os dados (4, 8 ou 12)
   Tempo em escala log	liga/desliga a escala logarítmica nos gráficos de tempo
   Tipos de gráfico:
   Gráfico	O que mostra
   Tempo × Tamanho	tempo médio em função de n, uma linha para o serial e uma para cada nº de threads
   Tempo × Threads	tempo médio em função das threads, uma linha por tamanho
   Speedup × Threads	speedup em função das threads, uma linha por tamanho
   Eficiência × Threads	eficiência em função das threads, uma linha por tamanho
   Comparar algoritmos	os 4 algoritmos juntos: serial (tracejado) e melhor paralelo (contínuo)
   Speedup × Threads (por nº de núcleos)	uma linha para cada execução (4, 8 e 12 núcleos), no maior tamanho
   Melhor speedup × Núcleos	melhor speedup de cada algoritmo em cada nº de núcleos
   Passe o mouse sobre um ponto para ver o valor exato.
   Salvar PNG grava a imagem em alta resolução em `resultados/graficos/`.
---
10. Principais resultados
    Dados aleatórios, valores de 0 a 1.000.000, maior tamanho de cada algoritmo:
    Algoritmo	4 núcleos	8 núcleos	12 núcleos
    Quick Sort (n = 10 milhões)	1,93×	3,66×	3,57×
    Merge Sort (n = 10 milhões)	2,26×	3,08×	3,17×
    Counting Sort, valores até 1.000 (n = 10 milhões)	1,61×	1,99×	2,38×
    Quick Sort e Merge Sort ganham com a paralelização, mas o ganho cresce menos que o número de núcleos — coerente com a Lei de Amdahl, já que a primeira partição (Quick) e a última intercalação (Merge) usam uma só thread.
    Bubble Sort paralelo fica mais lento a cada thread adicionada: o custo de sincronizar todas as threads a cada fase supera o ganho de dividir o trabalho.
    Counting Sort só escala bem com intervalo de valores pequeno; com intervalo grande, mais threads tornam-no mais lento que o serial.
    A natureza dos dados pesa muito: o Merge Sort fica ~30× mais rápido com dados já ordenados.
    A análise completa está no relatório do trabalho.
---
11. Como adicionar um novo algoritmo
    Exemplo: trocar o Bubble Sort pelo Insertion Sort.
    Crie `InsertionSortSerial` em `algoritmos/serial/` e `InsertionSortParalelo` em `algoritmos/paralelo/`, ambos implementando a interface `Ordenador`.
    Adicione uma linha no enum `benchmark/Algoritmo.java`:
```java
   INSERTION {
       @Override public Ordenador serial() { return new InsertionSortSerial(); }
       @Override public Ordenador paralelo(int t) { return new InsertionSortParalelo(t); }
   },
   ```
Inclua os tamanhos dele nos perfis, em `Main.criarConfig`.
O benchmark, os CSVs e os gráficos passam a incluir o novo algoritmo automaticamente.
---
Autores
Autor 1 — Guilherme Paiva de Medeiros
Autor 2 — Henrique Chaves Macedo de Sousa
Trabalho desenvolvido para o curso de Engenharia da Computação da Universidade de Fortaleza (UNIFOR).