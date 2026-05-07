package br.com.sindico.app.condominio;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.br.CNPJ;

public class CondominioForm {

    @NotBlank(message = "Nome do condominio e obrigatorio")
    @Size(min = 3, max = 150, message = "Nome deve ter entre 3 e 150 caracteres")
    private String nome;

    @CNPJ(message = "CNPJ invalido")
    private String cnpj;

    @Size(max = 500, message = "Endereco deve ter no maximo 500 caracteres")
    private String endereco;

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getCnpj() {
        return cnpj;
    }

    public void setCnpj(String cnpj) {
        this.cnpj = blankToNull(cnpj);
    }

    public String getEndereco() {
        return endereco;
    }

    public void setEndereco(String endereco) {
        this.endereco = endereco;
    }

    private static String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
