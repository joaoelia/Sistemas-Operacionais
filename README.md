# Sistema de Arquivos Virtual POSIX (Trabalho de Sistemas Operacionais)

## 🧩 Visão Geral

Este projeto é uma simulação de um sistema de arquivos virtual baseado nas chamadas POSIX, desenvolvido em Java como parte de um trabalho prático da disciplina de Sistemas Operacionais.

Foi implementada uma estrutura de árvore de diretórios e arquivos, incluindo suporte a permissões de acesso, blocos de dados, e um menu interativo para testes.

---

## ✅ Funcionalidades POSIX implementadas

| Comando | Descrição                                          |
| ------- | -------------------------------------------------- |
| `mkdir` | Cria diretórios, com verificação de permissões     |
| `touch` | Cria arquivos vazios com controle de permissão     |
| `ls`    | Lista arquivos e diretórios (com opção recursiva)  |
| `cp`    | Copia arquivos ou diretórios (recursivamente)      |
| `mv`    | Move ou renomeia arquivos e diretórios             |
| `write` | Escreve dados em arquivos usando blocos (1KB cada) |
| `read`  | Lê dados sequencialmente com buffer configurável   |
| `rm`    | Remove arquivos ou diretórios (recursivo opcional) |
| `chmod` | Configura permissões de leitura/escrita/execução   |

---

## 🔐 Controle de Permissões

* Cada arquivo/diretório tem um dono e permissões individuais por usuário.
* Root tem permissão total por padrão.
* Permissões seguem o formato POSIX: `rwx`, `rw-`, `---`, etc.
* Armazenamento via `HashMap<String, String>` no `MetaDados.java`.

---

## 🧠 Estrutura do Sistema de Arquivos

* Diretórios e arquivos são representados por:

  * `Diretorio.java`
  * `Arquivo.java`
  * `Bloco.java` (dados)
  * `MetaDados.java` (permissões, dono, nome)
* Representação em forma de árvore: a partir da raiz `/`
* Escrita segmentada em blocos para simular controle de fluxo em memória

---

## 📂 Estrutura do Projeto

```
Sistemas-Operacionais/
├── filesys/                 # Implementação principal
│   ├── exception/           # Exceções customizadas
│   ├── FileSystemImpl.java  # Sistema de arquivos (implementação real)
│   ├── FileSystem.java      # Proxy (camada intermediária)
│   ├── IFileSystem.java     # Interface obrigatória
│   └── ...                  # Arquivo, Diretório, Bloco, etc.
├── tests/
│   └── PermissionTest.java  # Testes automatizados com JUnit 5
├── lib/
│   └── junit-platform-console-standalone-1.12.0-RC2.jar
├── users/
│   └── users                # Lista de usuários e permissões
├── Main.java                # Menu interativo
├── Makefile (opcional)
└── README.md
```

---

## 🧪 Testes Automatizados (JUnit 5)

* Testes implementados usando `junit-platform-console-standalone`.
* Verificam:

  * Escrita e leitura
  * Permissões com e sem `chmod`
  * `mv` e `cp`
  * Exceções lançadas corretamente

### Execução dos testes (manual):

```bash
javac -cp "lib/*" -d bin tests/PermissionTest.java
java -jar lib/junit-platform-console-standalone-1.12.0-RC2.jar --classpath bin --scan-classpath
```

---

## 🖥️ Menu Interativo

Rodado por `Main.java`, permite testar todos os comandos diretamente via terminal.

### Exemplo:

```bash
$ java Main root
Comandos disponíveis:
1. chmod - Alterar permissões
2. mkdir - Criar diretório
...
```

---

## 🧾 Entrega

* [x] Interface `IFileSystem` mantida
* [x] Menu interativo funcional (`Main.java`)
* [x] Testes automatizados com cobertura de chamadas
* [x] Estrutura de arquivos e permissões simulada corretamente

---

## 👥 Autores

> João Paulo Fonseca Elias
> Lucas de Souza Pereira

---

## 📌 Observação Final

Este projeto simula com fidelidade as principais chamadas POSIX exigidas, incluindo armazenamento em blocos e controle de permissões de forma educativa, sem persistência em disco.

Pronto para ser avaliado via terminal e entrevistas presenciais.
