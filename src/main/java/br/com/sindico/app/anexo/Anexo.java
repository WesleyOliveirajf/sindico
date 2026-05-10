package br.com.sindico.app.anexo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "anexos")
public class Anexo {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "condominio_id", nullable = false)
    private UUID condominioId;

    @Column(name = "entidade_tipo", nullable = false, length = 30)
    private String entidadeTipo;

    @Column(name = "entidade_id", nullable = false)
    private UUID entidadeId;

    @Column(name = "nome_arquivo", nullable = false, length = 255)
    private String nomeArquivo;

    @Column(name = "url_arquivo", nullable = false)
    private String urlArquivo;

    @Column(name = "mime_type", length = 100)
    private String mimeType;

    @Column(name = "tamanho_bytes")
    private Long tamanhoBytes;

    @Column(name = "enviado_por")
    private UUID enviadoPor;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public UUID getId() { return id; }
    public UUID getCondominioId() { return condominioId; }
    public void setCondominioId(UUID condominioId) { this.condominioId = condominioId; }
    public String getEntidadeTipo() { return entidadeTipo; }
    public void setEntidadeTipo(String entidadeTipo) { this.entidadeTipo = entidadeTipo; }
    public UUID getEntidadeId() { return entidadeId; }
    public void setEntidadeId(UUID entidadeId) { this.entidadeId = entidadeId; }
    public String getNomeArquivo() { return nomeArquivo; }
    public void setNomeArquivo(String nomeArquivo) { this.nomeArquivo = nomeArquivo; }
    public String getUrlArquivo() { return urlArquivo; }
    public void setUrlArquivo(String urlArquivo) { this.urlArquivo = urlArquivo; }
    public String getMimeType() { return mimeType; }
    public void setMimeType(String mimeType) { this.mimeType = mimeType; }
    public Long getTamanhoBytes() { return tamanhoBytes; }
    public void setTamanhoBytes(Long tamanhoBytes) { this.tamanhoBytes = tamanhoBytes; }
    public UUID getEnviadoPor() { return enviadoPor; }
    public void setEnviadoPor(UUID enviadoPor) { this.enviadoPor = enviadoPor; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
