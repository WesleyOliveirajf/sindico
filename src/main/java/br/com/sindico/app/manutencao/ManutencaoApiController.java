package br.com.sindico.app.manutencao;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/manutencoes")
public class ManutencaoApiController {

    private final ManutencaoService manutencaoService;

    public ManutencaoApiController(ManutencaoService manutencaoService) {
        this.manutencaoService = manutencaoService;
    }

    @GetMapping
    public List<ManutencaoResponse> listar() {
        return manutencaoService.listarDoCondominioAtual().stream().map(ManutencaoResponse::from).toList();
    }

    @PostMapping
    public ResponseEntity<ManutencaoResponse> criar(@Valid @RequestBody ManutencaoRequest request) {
        Manutencao m = manutencaoService.criar(request);
        ManutencaoResponse response = ManutencaoResponse.from(m);
        return ResponseEntity.created(URI.create("/api/manutencoes/" + response.id())).body(response);
    }

    @PutMapping("/{id}")
    public ManutencaoResponse atualizar(@PathVariable UUID id, @Valid @RequestBody ManutencaoRequest request) {
        return ManutencaoResponse.from(manutencaoService.atualizar(id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletar(@PathVariable UUID id) {
        manutencaoService.deletar(id);
    }
}
