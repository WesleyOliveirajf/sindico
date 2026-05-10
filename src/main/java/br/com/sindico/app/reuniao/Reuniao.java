package br.com.sindico.app.reuniao;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "reunioes")
public class Reuniao {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "condominio_id", nullable = false)
    private UUID condominioId;

    @Column(name = "criado_por", nullable = false)
    private UUID criadoPor;

    @Column(nullable = false, length = 150)
    private String titulo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ReuniaoTipo tipo;

    @Column(name = "data_hora", nullable = false)
    private LocalDateTime dataHora;

    @Column(length = 150)
    private String local;

    @Column(length = 500)
    private String link;

    @Column(columnDefinition = "TEXT")
    private String pauta;

    @Column(columnDefinition = "TEXT")
    private String resumo;

    @Column(columnDefinition = "TEXT")
    private String decisoes;

    @Column(name = "pendencias_geradas", columnDefinition = "TEXT")
    private String pendenciasGeradas;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public UUID getId() { return id; }
    public UUID getCondominioId() { return condominioId; }
    public void setCondominioId(UUID condominioId) { this.condominioId = condominioId; }
    public UUID getCriadoPor() { return criadoPor; }
    public void setCriadoPor(UUID criadoPor) { this.criadoPor = criadoPor; }
    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }
    public ReuniaoTipo getTipo() { return tipo; }
    public void setTipo(ReuniaoTipo tipo) { this.tipo = tipo; }
    public LocalDateTime getDataHora() { return dataHora; }
    public void setDataHora(LocalDateTime dataHora) { this.dataHora = dataHora; }
    public String getLocal() { return local; }
    public void setLocal(String local) { this.local = local; }
    public String getLink() { return link; }
    public void setLink(String link) { this.link = link; }
    public String getPauta() { return pauta; }
    public void setPauta(String pauta) { this.pauta = pauta; }
    public String getResumo() { return resumo; }
    public void setResumo(String resumo) { this.resumo = resumo; }
    public String getDecisoes() { return decisoes; }
    public void setDecisoes(String decisoes) { this.decisoes = decisoes; }
    public String getPendenciasGeradas() { return pendenciasGeradas; }
    public void setPendenciasGeradas(String pendenciasGeradas) { this.pendenciasGeradas = pendenciasGeradas; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
