package br.com.sindico.app.prestador;

import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/prestadores")
public class PrestadorServicoApiController {

    private final PrestadorServicoService prestadorServicoService;
    private final PrestadorHistoricoCodec historicoCodec;

    public PrestadorServicoApiController(
            PrestadorServicoService prestadorServicoService,
            PrestadorHistoricoCodec historicoCodec) {
        this.prestadorServicoService = prestadorServicoService;
        this.historicoCodec = historicoCodec;
    }

    @GetMapping
    public List<PrestadorServicoResponse> listarAtivos() {
        return prestadorServicoService.listarDoCondominioAtual()
                .stream()
                .map(p -> PrestadorServicoResponse.from(p, historicoCodec.decode(p.getHistoricoServicos())))
                .toList();
    }

    @PostMapping
    public ResponseEntity<PrestadorServicoResponse> criar(@Valid @RequestBody PrestadorServicoRequest request) {
        NovoPrestadorForm form = new NovoPrestadorForm();
        form.setNome(request.nome());
        form.setTelefone(request.telefone());
        form.setHistoricoServicos(historicoCodec.encode(toHistoricoItens(request.historicoItens()), request.historicoServicos()));

        PrestadorServico prestador = prestadorServicoService.criar(form);
        PrestadorServicoResponse response = PrestadorServicoResponse.from(
                prestador,
                historicoCodec.decode(prestador.getHistoricoServicos()));

        return ResponseEntity
                .created(URI.create("/api/prestadores/" + response.id()))
                .body(response);
    }

    @PutMapping("/{prestadorId}")
    public ResponseEntity<?> atualizar(
            @PathVariable UUID prestadorId,
            @Valid @RequestBody PrestadorServicoRequest request) {
        try {
            AtualizarPrestadorForm form = new AtualizarPrestadorForm();
            form.setNome(request.nome());
            form.setTelefone(request.telefone());
            form.setHistoricoServicos(historicoCodec.encode(toHistoricoItens(request.historicoItens()), request.historicoServicos()));

            PrestadorServico prestador = prestadorServicoService.atualizar(prestadorId, form);
            return ResponseEntity.ok(PrestadorServicoResponse.from(
                    prestador,
                    historicoCodec.decode(prestador.getHistoricoServicos())));
        } catch (EntityNotFoundException ex) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{prestadorId}/inativar")
    public ResponseEntity<?> inativar(@PathVariable UUID prestadorId) {
        try {
            prestadorServicoService.inativar(prestadorId);
            return ResponseEntity.noContent().build();
        } catch (EntityNotFoundException ex) {
            return ResponseEntity.notFound().build();
        }
    }

    private List<PrestadorHistoricoItem> toHistoricoItens(List<PrestadorServicoRequest.HistoricoItemRequest> itens) {
        if (itens == null || itens.isEmpty()) {
            return List.of();
        }
        List<PrestadorHistoricoItem> historico = itens.stream()
                .map(item -> new PrestadorHistoricoItem(item.servico() == null ? "" : item.servico().trim(), item.valor()))
                .filter(item -> !item.servico().isBlank() && item.valor() != null)
                .toList();
        return historico;
    }
}
