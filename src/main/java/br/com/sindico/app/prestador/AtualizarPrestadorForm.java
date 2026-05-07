package br.com.sindico.app.prestador;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AtualizarPrestadorForm {

    @NotBlank(message = "Informe nome do prestador")
    @Size(max = 150, message = "Nome deve ter no maximo 150 caracteres")
    private String nome;

    @NotBlank(message = "Informe telefone ou WhatsApp")
    @Size(max = 30, message = "Telefone deve ter no maximo 30 caracteres")
    private String telefone;

    @Size(max = 4000, message = "Historico deve ter no maximo 4000 caracteres")
    private String historicoServicos;

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
}
