package br.com.sindico.app.gasto;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "gasto_parcelas")
public class GastoParcela {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "gasto_id", nullable = false)
    private UUID gastoId;

    @Column(nullable = false)
    private Integer numero;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal valor;

    @Column(nullable = false)
    private boolean paga;

    @Column(name = "paga_em")
    private LocalDateTime pagaEm;

    public UUID getId() { return id; }
    public UUID getGastoId() { return gastoId; }
    public void setGastoId(UUID gastoId) { this.gastoId = gastoId; }
    public Integer getNumero() { return numero; }
    public void setNumero(Integer numero) { this.numero = numero; }
    public BigDecimal getValor() { return valor; }
    public void setValor(BigDecimal valor) { this.valor = valor; }
    public boolean isPaga() { return paga; }
    public void setPaga(boolean paga) { this.paga = paga; }
    public LocalDateTime getPagaEm() { return pagaEm; }
    public void setPagaEm(LocalDateTime pagaEm) { this.pagaEm = pagaEm; }
}
