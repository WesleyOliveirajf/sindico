package br.com.sindico.app.prestador;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "prestadores_servico")
public class PrestadorServico {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "condominio_id", nullable = false)
    private UUID condominioId;

    @Column(nullable = false, length = 150)
    private String nome;

    @Column(nullable = false, length = 30)
    private String telefone;

    @Column(name = "historico_servicos", columnDefinition = "TEXT")
    private String historicoServicos;

    @Column(nullable = false)
    private boolean ativo = true;

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

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getTelefone() {
        return telefone;
    }

    public void setTelefone(String telefone) {
        this.telefone = telefone;
    }

    public String getHistoricoServicos() {
        return historicoServicos;
    }

    public void setHistoricoServicos(String historicoServicos) {
        this.historicoServicos = historicoServicos;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public void setAtivo(boolean ativo) {
        this.ativo = ativo;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
