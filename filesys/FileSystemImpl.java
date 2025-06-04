package filesys;

import exception.CaminhoJaExistenteException;
import exception.CaminhoNaoEncontradoException;
import exception.PermissaoException;

import java.util.*;

public final class FileSystemImpl implements IFileSystem {
    private static final String ROOT_USER = "root";
    private static final int TAMANHO_BLOCO = 1024; // 1KB por bloco

    private Diretorio raiz;
    private Set<String> usuarios;

    public FileSystemImpl() {
        this.raiz = new Diretorio("/", ROOT_USER);
        this.usuarios = new HashSet<>();
        this.usuarios.add(ROOT_USER);
    }

    @Override
    public void mkdir(String caminho, String nome) throws CaminhoJaExistenteException, PermissaoException {
        try {
            Diretorio dirPai = navegarParaDiretorio(caminho);

            for (Diretorio sub : dirPai.getSubDiretorios()) {
                if (sub.getMetaDados().getNome().equals(nome)) {
                    throw new CaminhoJaExistenteException("Diretório já existe");
                }
            }

            String dono = dirPai.getMetaDados().getDono();
            String permissao = dirPai.getMetaDados().getPermissao(dono);
            if (!permissao.contains("w") && !ROOT_USER.equals(dono)) {
                throw new PermissaoException("Permissão negada para criar diretório");
            }

            Diretorio novoDir = new Diretorio(nome, dono);
            dirPai.adicionarSubDiretorio(novoDir);

        } catch (CaminhoNaoEncontradoException e) {
            throw new PermissaoException("Diretório pai não encontrado");
        }
    }

    @Override
    public void chmod(String caminho, String usuario, String usuarioAlvo, String permissao)
            throws CaminhoNaoEncontradoException, PermissaoException {
        Diretorio dir = navegarParaDiretorio(caminho);
        if (!dir.getMetaDados().getDono().equals(usuario) && !usuario.equals(ROOT_USER)) {
            throw new PermissaoException("Permissão negada para alterar permissões");
        }
        dir.getMetaDados().setPermissao(usuarioAlvo, permissao);
    }

    @Override
    public void rm(String caminho, String usuario, boolean recursivo)
            throws CaminhoNaoEncontradoException, PermissaoException {
        String[] partes = caminho.split("/");
        String nomeAlvo = partes[partes.length - 1];
        String caminhoPai = String.join("/", Arrays.copyOfRange(partes, 0, partes.length - 1));
        Diretorio pai = caminhoPai.isEmpty() ? raiz : navegarParaDiretorio(caminhoPai);

        for (Iterator<Arquivo> it = pai.getArquivos().iterator(); it.hasNext(); ) {
            Arquivo arq = it.next();
            if (arq.getMetaDados().getNome().equals(nomeAlvo)) {
                if (!usuario.equals(ROOT_USER) && !arq.getMetaDados().getDono().equals(usuario)) {
                    throw new PermissaoException("Permissão negada para remover o arquivo");
                }
                it.remove();
                return;
            }
        }

        for (Iterator<Diretorio> it = pai.getSubDiretorios().iterator(); it.hasNext(); ) {
            Diretorio dir = it.next();
            if (dir.getMetaDados().getNome().equals(nomeAlvo)) {
                if (!usuario.equals(ROOT_USER) && !dir.getMetaDados().getDono().equals(usuario)) {
                    throw new PermissaoException("Permissão negada para remover o diretório");
                }
                if (!recursivo && (!dir.getArquivos().isEmpty() || !dir.getSubDiretorios().isEmpty())) {
                    throw new PermissaoException("Diretório não está vazio e remoção recursiva não foi autorizada");
                }
                it.remove();
                return;
            }
        }

        throw new CaminhoNaoEncontradoException("Arquivo ou diretório não encontrado");
    }

    @Override
    public void touch(String caminho, String usuario) throws CaminhoJaExistenteException, PermissaoException {
        String[] partes = caminho.split("/");
        String nomeArquivo = partes[partes.length - 1];
        String caminhoPai = String.join("/", Arrays.copyOfRange(partes, 0, partes.length - 1));

        try {
            Diretorio dirPai = caminhoPai.isEmpty() ? raiz : navegarParaDiretorio(caminhoPai);

            for (Arquivo arq : dirPai.getArquivos()) {
                if (arq.getMetaDados().getNome().equals(nomeArquivo)) {
                    throw new CaminhoJaExistenteException("Arquivo já existe");
                }
            }

            String permissao = dirPai.getMetaDados().getPermissao(usuario);
            if (!permissao.contains("w") && !usuario.equals(ROOT_USER)) {
                throw new PermissaoException("Permissão negada para criar arquivo");
            }

            Arquivo novoArquivo = new Arquivo(nomeArquivo, usuario);
            dirPai.adicionarArquivo(novoArquivo);

        } catch (CaminhoNaoEncontradoException e) {
            throw new PermissaoException("Diretório pai não encontrado");
        }
    }

    @Override
    public void write(String caminho, String usuario, boolean anexar, byte[] buffer)
            throws CaminhoNaoEncontradoException, PermissaoException {
        String[] partes = caminho.split("/");
        String nomeArquivo = partes[partes.length - 1];
        String caminhoPai = String.join("/", Arrays.copyOfRange(partes, 0, partes.length - 1));

        Diretorio dirPai = caminhoPai.isEmpty() ? raiz : navegarParaDiretorio(caminhoPai);
        Arquivo arquivo = null;

        for (Arquivo arq : dirPai.getArquivos()) {
            if (arq.getMetaDados().getNome().equals(nomeArquivo)) {
                arquivo = arq;
                break;
            }
        }

        if (arquivo == null) {
            throw new CaminhoNaoEncontradoException("Arquivo não encontrado");
        }

        String permissao = arquivo.getMetaDados().getPermissao(usuario);
        if (!permissao.contains("w") && !usuario.equals(ROOT_USER)) {
            throw new PermissaoException("Permissão negada para escrever no arquivo");
        }

        if (!anexar) {
            arquivo.getBlocos().clear();
            arquivo.getMetaDados().setTamanho(0);
        }

        int offset = 0;
        while (offset < buffer.length) {
            int chunkSize = Math.min(TAMANHO_BLOCO, buffer.length - offset);
            Byte[] chunk = new Byte[chunkSize];
            for (int i = 0; i < chunkSize; i++) {
                chunk[i] = buffer[offset + i];
            }
            Bloco bloco = new Bloco(chunkSize);
            bloco.setDados(chunk);
            arquivo.addBloco(bloco);
            offset += chunkSize;
        }
    }

    @Override
    public void read(String caminho, String usuario, byte[] buffer)
            throws CaminhoNaoEncontradoException, PermissaoException {
        String[] partes = caminho.split("/");
        String nomeArquivo = partes[partes.length - 1];
        String caminhoPai = String.join("/", Arrays.copyOfRange(partes, 0, partes.length - 1));

        Diretorio dirPai = caminhoPai.isEmpty() ? raiz : navegarParaDiretorio(caminhoPai);
        Arquivo arquivo = null;

        for (Arquivo arq : dirPai.getArquivos()) {
            if (arq.getMetaDados().getNome().equals(nomeArquivo)) {
                arquivo = arq;
                break;
            }
        }

        if (arquivo == null) {
            throw new CaminhoNaoEncontradoException("Arquivo não encontrado");
        }

        String permissao = arquivo.getMetaDados().getPermissao(usuario);
        if (!permissao.contains("r") && !usuario.equals(ROOT_USER)) {
            throw new PermissaoException("Permissão negada para leitura");
        }

        int bufferIndex = 0;
        for (Bloco bloco : arquivo.getBlocos()) {
            Byte[] dados = bloco.getDados();
            for (Byte b : dados) {
                if (bufferIndex >= buffer.length) return;
                buffer[bufferIndex++] = b;
            }
        }
    }

    @Override
    public void mv(String caminhoAntigo, String caminhoNovo, String usuario)
            throws CaminhoNaoEncontradoException, PermissaoException {
        // TODO: Implementar movimentação/renomeação de arquivos ou diretórios
    }

    @Override
    public void ls(String caminho, String usuario, boolean recursivo)
            throws CaminhoNaoEncontradoException, PermissaoException {
        // TODO: Implementar listagem de arquivos e diretórios (recursiva opcional)
    }

    @Override
    public void cp(String caminhoOrigem, String caminhoDestino, String usuario, boolean recursivo)
            throws CaminhoNaoEncontradoException, PermissaoException {
        // TODO: Implementar cópia de arquivos e diretórios (recursiva opcional)
    }

    public void addUser(String user) {
        usuarios.add(user);
    }

    private Diretorio navegarParaDiretorio(String caminho) throws CaminhoNaoEncontradoException {
        if (!caminho.startsWith("/")) {
            throw new CaminhoNaoEncontradoException("Caminho inválido (deve começar com '/')");
        }

        if (caminho.equals("/")) return raiz;

        String[] partes = caminho.split("/");
        Diretorio atual = raiz;

        for (int i = 1; i < partes.length; i++) {
            String nome = partes[i];
            Optional<Diretorio> encontrado = atual.getSubDiretorios().stream()
                    .filter(d -> d.getMetaDados().getNome().equals(nome))
                    .findFirst();

            if (encontrado.isPresent()) {
                atual = encontrado.get();
            } else {
                throw new CaminhoNaoEncontradoException("Diretório não encontrado: " + nome);
            }
        }

        return atual;
    }
} 