package br.com.sindico.app.cadastro;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CadastroForm {

    @NotBlank(message = "Informe seu nome")
    @Size(max = 150, message = "Nome deve ter no maximo 150 caracteres")
    private String nome;

    @NotBlank(message = "Informe o e-mail")
    @Email(message = "E-mail invalido")
    @Size(max = 150, message = "E-mail deve ter no maximo 150 caracteres")
    private String email;

    @NotBlank(message = "Informe o nome do condominio")
    @Size(max = 150, message = "Nome do condominio deve ter no maximo 150 caracteres")
    private String nomeCondominio;

    @NotBlank(message = "Informe a senha")
    @Size(min = 8, max = 100, message = "Senha deve ter entre 8 e 100 caracteres")
    private String senha;

    @NotBlank(message = "Confirme a senha")
    private String confirmarSenha;

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getNomeCondominio() { return nomeCondominio; }
    public void setNomeCondominio(String nomeCondominio) { this.nomeCondominio = nomeCondominio; }

    public String getSenha() { return senha; }
    public void setSenha(String senha) { this.senha = senha; }

    public String getConfirmarSenha() { return confirmarSenha; }
    public void setConfirmarSenha(String confirmarSenha) { this.confirmarSenha = confirmarSenha; }
}
