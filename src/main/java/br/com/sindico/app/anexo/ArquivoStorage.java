package br.com.sindico.app.anexo;

import java.io.InputStream;
import org.springframework.core.io.Resource;

/**
 * Armazenamento fisico dos arquivos de anexo.
 * A referencia retornada por {@link #salvar} e gravada em {@code anexos.url_arquivo}
 * e depois usada em {@link #carregar}.
 */
public interface ArquivoStorage {

    /**
     * @param chave caminho relativo e ja sanitizado (ex.: {condominio}/{tipo}/{entidade}/{uuid}.pdf)
     * @return referencia persistivel do arquivo
     */
    String salvar(String chave, InputStream conteudo, long tamanhoBytes, String contentType);

    /** Retorna o conteudo do arquivo ou lanca EntityNotFoundException se nao existir. */
    Resource carregar(String referencia);
}
