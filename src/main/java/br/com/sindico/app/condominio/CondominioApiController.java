package br.com.sindico.app.condominio;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/condominio")
public class CondominioApiController {

    private final CondominioService condominioService;

    public CondominioApiController(CondominioService condominioService) {
        this.condominioService = condominioService;
    }

    @GetMapping
    public CondominioResponse buscar() {
        CondominioForm form = condominioService.buscarFormAtual();
        return new CondominioResponse(form.getNome(), form.getCnpj(), form.getEndereco());
    }

    @PutMapping
    public CondominioResponse atualizar(@Valid @RequestBody CondominioForm form) {
        Condominio condominio = condominioService.atualizar(form);
        return new CondominioResponse(condominio.getNome(), condominio.getCnpj(), condominio.getEndereco());
    }

    public record CondominioResponse(String nome, String cnpj, String endereco) {}
}
