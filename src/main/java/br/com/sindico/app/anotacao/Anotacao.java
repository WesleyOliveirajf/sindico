package br.com.sindico.app.anotacao;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "anotacoes")
public class Anotacao {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "condominio_id", nullable = false)
    private UUID condominioId;

    @Column(nullable = false, length = 150)
    private String titulo;

    @Column(length = 50)
    private String categoria;

    @Column(columnDefinition = "TEXT")
    private String descricao;

    @Column(length = 200)
    private String referencia;

    @Column(name = "data_referencia")
    private LocalDate dataReferencia;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AnotacaoImportancia importancia = AnotacaoImportancia.NORMAL;

    @Column(name = "criado_por")
    private UUID criadoPor;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public UUID getId() {
        return id;
    }

    public UUID getCondominioId() {
        return condominioId;
    }

    public void setCondominioId(UUID condominioId) {
        this.condominioId = condominioId;
    }

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

    public UUID getCriadoPor() {
        return criadoPor;
    }

    public void setCriadoPor(UUID criadoPor) {
        this.criadoPor = criadoPor;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
