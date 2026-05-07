package br.com.sindico.app.morador;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public class NovoMoradorForm {

    @NotNull(message = "Selecione a unidade")
    private UUID unidadeId;

    @NotBlank(message = "Informe o nome")
    @Size(max = 150, message = "Nome deve ter no maximo 150 caracteres")
    private String nome;

    @Size(max = 150, message = "Email deve ter no maximo 150 caracteres")
    @Pattern(
            regexp = "^$|^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$",
            message = "Email invalido")
    private String email;

    @Size(max = 30, message = "Telefone deve ter no maximo 30 caracteres")
    private String telefone;

    @NotNull(message = "Selecione o papel")
    private MoradorPapel papel;

    @Size(max = 2000, message = "Observacoes devem ter no maximo 2000 caracteres")
    private String observacoes;

    public UUID getUnidadeId() {
        return unidadeId;
    }

    public void setUnidadeId(UUID unidadeId) {
        this.unidadeId = unidadeId;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getTelefone() {
        return telefone;
    }

    public void setTelefone(String telefone) {
        this.telefone = telefone;
    }

    public MoradorPapel getPapel() {
        return papel;
    }

    public void setPapel(MoradorPapel papel) {
        this.papel = papel;
    }

    public String getObservacoes() {
        return observacoes;
    }

    public void setObservacoes(String observacoes) {
        this.observacoes = observacoes;
    }
}
