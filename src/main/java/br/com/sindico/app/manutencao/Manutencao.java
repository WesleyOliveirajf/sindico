package br.com.sindico.app.manutencao;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "manutencoes")
public class Manutencao {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "condominio_id", nullable = false)
    private UUID condominioId;

    @Column(name = "ativo_id")
    private UUID ativoId;

    @Column(name = "fornecedor_id")
    private UUID fornecedorId;

    @Column(name = "criado_por", nullable = false)
    private UUID criadoPor;

    @Column(nullable = false, length = 150)
    private String titulo;

    @Column(columnDefinition = "TEXT")
    private String descricao;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ManutencaoTipo tipo;

    @Column(length = 50)
    private String categoria;

    @Column(length = 30)
    private String prioridade;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ManutencaoStatus status;

    @Column(name = "data_ocorrencia")
    private LocalDate dataOcorrencia;

    @Column(name = "data_execucao")
    private LocalDate dataExecucao;

    @Column(name = "custo_previsto", precision = 12, scale = 2)
    private BigDecimal custoPrevisto;

    @Column(name = "custo_realizado", precision = 12, scale = 2)
    private BigDecimal custoRealizado;

    @Column(length = 150)
    private String local;

    @Column(name = "responsavel_interno", length = 150)
    private String responsavelInterno;

    @Column(columnDefinition = "TEXT")
    private String observacoes;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public UUID getId() { return id; }
    public UUID getCondominioId() { return condominioId; }
    public void setCondominioId(UUID condominioId) { this.condominioId = condominioId; }
    public UUID getAtivoId() { return ativoId; }
    public void setAtivoId(UUID ativoId) { this.ativoId = ativoId; }
    public UUID getFornecedorId() { return fornecedorId; }
    public void setFornecedorId(UUID fornecedorId) { this.fornecedorId = fornecedorId; }
    public UUID getCriadoPor() { return criadoPor; }
    public void setCriadoPor(UUID criadoPor) { this.criadoPor = criadoPor; }
    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }
    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
    public ManutencaoTipo getTipo() { return tipo; }
    public void setTipo(ManutencaoTipo tipo) { this.tipo = tipo; }
    public String getCategoria() { return categoria; }
    public void setCategoria(String categoria) { this.categoria = categoria; }
    public String getPrioridade() { return prioridade; }
    public void setPrioridade(String prioridade) { this.prioridade = prioridade; }
    public ManutencaoStatus getStatus() { return status; }
    public void setStatus(ManutencaoStatus status) { this.status = status; }
    public LocalDate getDataOcorrencia() { return dataOcorrencia; }
    public void setDataOcorrencia(LocalDate dataOcorrencia) { this.dataOcorrencia = dataOcorrencia; }
    public LocalDate getDataExecucao() { return dataExecucao; }
    public void setDataExecucao(LocalDate dataExecucao) { this.dataExecucao = dataExecucao; }
    public BigDecimal getCustoPrevisto() { return custoPrevisto; }
    public void setCustoPrevisto(BigDecimal custoPrevisto) { this.custoPrevisto = custoPrevisto; }
    public BigDecimal getCustoRealizado() { return custoRealizado; }
    public void setCustoRealizado(BigDecimal custoRealizado) { this.custoRealizado = custoRealizado; }
    public String getLocal() { return local; }
    public void setLocal(String local) { this.local = local; }
    public String getResponsavelInterno() { return responsavelInterno; }
    public void setResponsavelInterno(String responsavelInterno) { this.responsavelInterno = responsavelInterno; }
    public String getObservacoes() { return observacoes; }
    public void setObservacoes(String observacoes) { this.observacoes = observacoes; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
