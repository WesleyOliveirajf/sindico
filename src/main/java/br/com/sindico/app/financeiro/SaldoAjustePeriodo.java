package br.com.sindico.app.financeiro;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "saldo_ajuste_periodo")
public class SaldoAjustePeriodo {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "condominio_id", nullable = false)
    private UUID condominioId;

    @Column(nullable = false)
    private int ano;

    @Column(nullable = false)
    private int mes;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal valor;

    private String observacao;

    @Column(name = "atualizado_por")
    private UUID atualizadoPor;

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

    public int getAno() {
        return ano;
    }

    public void setAno(int ano) {
        this.ano = ano;
    }

    public int getMes() {
        return mes;
    }

    public void setMes(int mes) {
        this.mes = mes;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public void setValor(BigDecimal valor) {
        this.valor = valor;
    }

    public String getObservacao() {
        return observacao;
    }

    public void setObservacao(String observacao) {
        this.observacao = observacao;
    }

    public UUID getAtualizadoPor() {
        return atualizadoPor;
    }

    public void setAtualizadoPor(UUID atualizadoPor) {
        this.atualizadoPor = atualizadoPor;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
