package br.com.sindico.app.reuniao;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "participantes_reuniao")
public class ParticipanteReuniao {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reuniao_id", nullable = false)
    private Reuniao reuniao;

    @Column(nullable = false, length = 150)
    private String nome;

    @Column(length = 100)
    private String cargo;

    @Column(nullable = false)
    private boolean presente = true;

    public UUID getId() { return id; }
    public Reuniao getReuniao() { return reuniao; }
    public void setReuniao(Reuniao reuniao) { this.reuniao = reuniao; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getCargo() { return cargo; }
    public void setCargo(String cargo) { this.cargo = cargo; }
    public boolean isPresente() { return presente; }
    public void setPresente(boolean presente) { this.presente = presente; }
}
