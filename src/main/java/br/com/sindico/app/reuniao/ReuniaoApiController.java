package br.com.sindico.app.reuniao;

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
@RequestMapping("/api/reunioes")
public class ReuniaoApiController {

    private final ReuniaoService reuniaoService;

    public ReuniaoApiController(ReuniaoService reuniaoService) {
        this.reuniaoService = reuniaoService;
    }

    @GetMapping
    public List<ReuniaoResponse> listar() {
        return reuniaoService.listarDoCondominioAtual();
    }

    @PostMapping
    public ResponseEntity<ReuniaoResponse> criar(@Valid @RequestBody ReuniaoRequest request) {
        ReuniaoResponse response = reuniaoService.criar(request);
        return ResponseEntity.created(URI.create("/api/reunioes/" + response.id())).body(response);
    }

    @PutMapping("/{id}")
    public ReuniaoResponse atualizar(@PathVariable UUID id, @Valid @RequestBody ReuniaoRequest request) {
        return reuniaoService.atualizar(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletar(@PathVariable UUID id) {
        reuniaoService.deletar(id);
    }
}
