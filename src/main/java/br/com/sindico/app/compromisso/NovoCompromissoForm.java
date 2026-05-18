package br.com.sindico.app.compromisso;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

public class NovoCompromissoForm {

    @NotBlank(message = "Informe um titulo")
    @Size(max = 150, message = "Titulo deve ter no maximo 150 caracteres")
    private String titulo;

    @Size(max = 2000, message = "Descricao deve ter no maximo 2000 caracteres")
    private String descricao;

    @NotNull(message = "Selecione o tipo")
    private CompromissoTipo tipo;

    @NotNull(message = "Informe data e hora de inicio")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime inicioEm;

    // fimEm opcional — preenchido automaticamente quando o compromisso for concluído
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime fimEm;

    @Size(max = 150, message = "Local deve ter no maximo 150 caracteres")
    private String local;

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public CompromissoTipo getTipo() {
        return tipo;
    }

    public void setTipo(CompromissoTipo tipo) {
        this.tipo = tipo;
    }

    public LocalDateTime getInicioEm() {
        return inicioEm;
    }

    public void setInicioEm(LocalDateTime inicioEm) {
        this.inicioEm = inicioEm;
    }

    public LocalDateTime getFimEm() {
        return fimEm;
    }

    public void setFimEm(LocalDateTime fimEm) {
        this.fimEm = fimEm;
    }

    public String getLocal() {
        return local;
    }

    public void setLocal(String local) {
        this.local = local;
    }
}
