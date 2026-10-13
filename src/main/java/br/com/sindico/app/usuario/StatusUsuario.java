package br.com.sindico.app.usuario;

/**
 * Status de conta persistidos em {@code usuarios.status}. Fonte unica dos valores; a coluna
 * continua texto, entao use {@link #valor()} ao gravar e {@link #corresponde(String)} ao comparar.
 */
public enum StatusUsuario {
    ATIVO("ativo"),
    INATIVO("inativo"),
    PENDENTE("pendente");

    private final String valor;

    StatusUsuario(String valor) {
        this.valor = valor;
    }

    public String valor() {
        return valor;
    }

    public boolean corresponde(String status) {
        return valor.equals(status);
    }
}
