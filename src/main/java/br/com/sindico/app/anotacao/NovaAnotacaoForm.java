package br.com.sindico.app.anotacao;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;

public class NovaAnotacaoForm {

    @NotBlank(message = "Informe um titulo")
    @Size(max = 150, message = "Titulo deve ter no maximo 150 caracteres")
    private String titulo;

    @Size(max = 50, message = "Categoria deve ter no maximo 50 caracteres")
    private String categoria;

    @Size(max = 4000, message = "Descricao deve ter no maximo 4000 caracteres")
    private String descricao;

    @Size(max = 200, message = "Referencia deve ter no maximo 200 caracteres")
    private String referencia;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dataReferencia;

    @NotNull(message = "Selecione a importancia")
    private AnotacaoImportancia importancia = AnotacaoImportancia.NORMAL;

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getCategoria() {
        return categoria;
    }

    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public String getReferencia() {
        return referencia;
    }

    public void setReferencia(String referencia) {
        this.referencia = referencia;
    }

    public LocalDate getDataReferencia() {
        return dataReferencia;
    }

    public void setDataReferencia(LocalDate dataReferencia) {
        this.dataReferencia = dataReferencia;
    }

    public AnotacaoImportancia getImportancia() {
        return importancia;
    }

    public void setImportancia(AnotacaoImportancia importancia) {
        this.importancia = importancia;
    }
}
