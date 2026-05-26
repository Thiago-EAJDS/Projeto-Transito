# 🚦 Projeto Trânsito — Simulador de Tráfego Urbano

> Simulador visual de tráfego urbano desenvolvido em Java com JavaFX, onde veículos se movem por um mapa representado como grafo de vias, utilizando algoritmos clássicos de estruturas de dados.

---

## 📋 Sobre o Projeto

O **Projeto Trânsito** é um simulador de tráfego urbano que modela o fluxo de veículos em um mapa de vias. O mapa é representado internamente como um **grafo**, onde os vértices são cruzamentos/pontos de parada e as arestas são os trechos de via que os conectam. Os veículos percorrem esse grafo com base em algoritmos de menor caminho, simulando o comportamento real de um sistema viário.

A interface gráfica, construída com **JavaFX**, permite visualizar o mapa e os veículos em movimento, além de configurar os parâmetros da simulação antes de iniciá-la.

> ⚠️ O projeto está em processo desativado de mudança, aprimoramento e documentação. Irei alterá-lo em breve.

---

## ✨ Funcionalidades

- 🗺️ Visualização do mapa de vias em interface gráfica
- 🚗 Simulação de veículos se movendo pelo grafo de vias
- ⚙️ Configuração dos parâmetros da simulação via interface
- 🔍 Cálculo de rotas com algoritmo de menor caminho (Dijkstra / BFS)
- 📦 Gerenciamento de veículos com filas e listas
- 💾 Serialização de dados com JSON (Gson)

---

## 🛠️ Tecnologias Utilizadas

| Tecnologia | Versão | Uso |
|---|---|---|
| Java | 11 / 16 | Linguagem principal |
| JavaFX | 17.0.2 | Interface gráfica e visualização |
| Maven | — | Gerenciamento de dependências e build |

---

## 🧠 Estruturas de Dados e Algoritmos

O projeto aplica na prática conceitos fundamentais de estruturas de dados:

- **Grafos** — representação do mapa de vias (vértices = cruzamentos, arestas = trechos de rua)
- **Algoritmo de menor caminho** — cálculo das rotas percorridas pelos veículos (Dijkstra / BFS)
- **Filas e Listas** — controle e gerenciamento do fluxo de veículos
- **Árvores** — estruturação hierárquica de dados da simulação

---

## ▶️ Como Executar

### Pré-requisitos

- [Java JDK 11+](https://www.oracle.com/java/technologies/downloads/)
- [Maven 3.6+](https://maven.apache.org/download.cgi)

### Passos

1. Clone o repositório:
   ```bash
   git clone https://github.com/Thiago-EAJDS/Projeto-Transito.git
   cd Projeto-Transito
   ```

2. Compile o projeto com Maven:
   ```bash
   ./mvnw clean install
   ```

3. Execute a aplicação:
   ```bash
   ./mvnw javafx:run
   ```

> **Windows:** utilize `mvnw.cmd` no lugar de `./mvnw`

---

## 🖥️ Interface

A interface gráfica exibe o mapa de vias e a movimentação dos veículos em tempo real. As **configurações da simulação** (como quantidade de veículos, velocidade, etc.) podem ser ajustadas pelo usuário antes ou durante a execução, mas o layout do mapa em si é fixo.

---

## 🚧 Status do Projeto

```
Desenvolvimento Parado🔄
```

- [x] Estrutura base do projeto
- [x] Representação do mapa como grafo
- [x] Algoritmo de menor caminho
- [x] Interface gráfica com JavaFX
- [ ] Documentação completa
- [ ] Testes automatizados
- [ ] Melhorias de desempenho

---

## 📄 Licença

Este projeto está sob desenvolvimento. Licença a ser definida.

---
