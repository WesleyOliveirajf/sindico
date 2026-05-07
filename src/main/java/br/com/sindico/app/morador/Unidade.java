package br.com.sindico.app.morador;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "unidades")
public class Unidade {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "condominio_id", nullable = false)
    private UUID condominioId;

    @Column(nullable = false, length = 30)
    private String bloco = "";

    @Column(nullable = false, length = 30)
    private String numero;

    @Column(length = 100)
    private String complemento;

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

    public String getBloco() {
        return bloco;
    }

    public void setBloco(String bloco) {
        this.bloco = bloco == null ? "" : bloco.trim();
    }

    public String getNumero() {
        return numero;
    }

    public void setNumero(String numero) {
        this.numero = numero;
    }

    public String getComplemento() {
        return complemento;
    }

    public void setComplemento(String complemento) {
        this.complemento = complemento;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    /** Ex.: "Torre A · 1201" ou "101"; complemento entre parenteses se existir */
    public String getRotulo() {
        String b = getBloco();
        String base = b.isEmpty() ? getNumero() : b + " · " + getNumero();
        if (complemento != null && !complemento.isBlank()) {
            return base + " (" + complemento.trim() + ")";
        }
        return base;
    }
}
