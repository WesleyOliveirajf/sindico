package br.com.sindico.app.compromisso;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class CompromissoResponse {

    private Long id;
    private String titulo;
    private String descricao;
    private LocalDateTime inicio;
    private LocalDateTime fim;
    private String local;
    private String participantes;
    private String observacoes;
    private String condominioNome;
    private Long condominioId;
    private String status;
    private boolean concluido;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }
    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
    public LocalDateTime getInicio() { return inicio; }
    public void setInicio(LocalDateTime inicio) { this.inicio = inicio; }
    public LocalDateTime getFim() { return fim; }
    public void setFim(LocalDateTime fim) { this.fim = fim; }
    public String getLocal() { return local; }
    public void setLocal(String local) { this.local = local; }
    public String getParticipantes() { return participantes; }
    public void setParticipantes(String participantes) { this.participantes = participantes; }
    public String getObservacoes() { return observacoes; }
    public void setObservacoes(String observacoes) { this.observacoes = observacoes; }
    public String getCondominioNome() { return condominioNome; }
    public void setCondominioNome(String condominioNome) { this.condominioNome = condominioNome; }
    public Long getCondominioId() { return condominioId; }
    public void setCondominioId(Long condominioId) { this.condominioId = condominioId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public boolean isConcluido() { return concluido; }
    public void setConcluido(boolean concluido) { this.concluido = concluido; }

    /** Só data dd/MM/yyyy — sem hora */
    public String getInicioFormatado() {
        if (inicio == null) return "";
        return inicio.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }

    /** Data+hora exata da conclusão */
    public String getFimFormatado() {
        if (fim == null) return "";
        return fim.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
    }
}

