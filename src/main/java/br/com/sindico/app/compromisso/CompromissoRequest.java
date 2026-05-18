package br.com.sindico.app.compromisso;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDate;

public class CompromissoRequest {

    @NotBlank(message = "Título é obrigatório")
    @Size(max = 200, message = "Título deve ter no máximo 200 caracteres")
    private String titulo;

    private String descricao;

    @NotNull(message = "Data de início é obrigatória")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate inicio;

    // fim removido — preenchido automaticamente ao concluir

    private String local;
    private String participantes;
    private String observacoes;

    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }
    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
    public LocalDate getInicio() { return inicio; }
    public void setInicio(LocalDate inicio) { this.inicio = inicio; }
    public String getLocal() { return local; }
    public void setLocal(String local) { this.local = local; }
    public String getParticipantes() { return participantes; }
    public void setParticipantes(String participantes) { this.participantes = participantes; }
    public String getObservacoes() { return observacoes; }
    public void setObservacoes(String observacoes) { this.observacoes = observacoes; }
}

