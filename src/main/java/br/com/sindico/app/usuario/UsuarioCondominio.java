package br.com.sindico.app.usuario;

import br.com.sindico.app.condominio.Condominio;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "usuarios_condominios")
public class UsuarioCondominio {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "condominio_id", nullable = false)
    private Condominio condominio;

    @Column(nullable = false, length = 30)
    private String perfil;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public UUID getCondominioId() {
        return condominio != null ? condominio.getId() : null;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public Condominio getCondominio() {
        return condominio;
    }

    public String getPerfil() {
        return perfil;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public void setCondominio(br.com.sindico.app.condominio.Condominio condominio) {
        this.condominio = condominio;
    }

    public void setPerfil(String perfil) {
        this.perfil = perfil;
    }
}
