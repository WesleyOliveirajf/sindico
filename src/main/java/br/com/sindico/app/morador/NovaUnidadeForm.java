package br.com.sindico.app.morador;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class NovaUnidadeForm {

    @Size(max = 30, message = "Bloco deve ter no maximo 30 caracteres")
    private String bloco;

    @NotBlank(message = "Informe o numero da unidade")
    @Size(max = 30, message = "Numero deve ter no maximo 30 caracteres")
    private String numero;

    @Size(max = 100, message = "Complemento deve ter no maximo 100 caracteres")
    private String complemento;

    public String getBloco() {
        return bloco;
    }

    public void setBloco(String bloco) {
        this.bloco = bloco;
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
}
