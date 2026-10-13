package br.com.sindico.app.auth;

/** Conta existe, mas ainda aguarda aprovacao do administrador; nenhum token deve ser emitido. */
public class CadastroPendenteException extends RuntimeException {

    public CadastroPendenteException() {
        super("Cadastro recebido. Aguarde a aprovação do administrador para acessar.");
    }
}
