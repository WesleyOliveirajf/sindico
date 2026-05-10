package br.com.sindico.app.reuniao;

import br.com.sindico.app.security.TenantAccessor;
import br.com.sindico.app.security.UsuarioTenantPrincipal;
import jakarta.persistence.EntityNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReuniaoService {

    private final ReuniaoRepository reuniaoRepository;
    private final ParticipanteReuniaoRepository participanteReuniaoRepository;
    private final TenantAccessor tenantAccessor;

    public ReuniaoService(
            ReuniaoRepository reuniaoRepository,
            ParticipanteReuniaoRepository participanteReuniaoRepository,
            TenantAccessor tenantAccessor) {
        this.reuniaoRepository = reuniaoRepository;
        this.participanteReuniaoRepository = participanteReuniaoRepository;
        this.tenantAccessor = tenantAccessor;
    }

    @Transactional(readOnly = true)
    public List<ReuniaoResponse> listarDoCondominioAtual() {
        List<Reuniao> reunioes = reuniaoRepository.findByCondominioIdOrderByDataHoraDesc(tenantAccessor.condominioAtual());
        List<ReuniaoResponse> out = new ArrayList<>();
        for (Reuniao reuniao : reunioes) {
            List<ParticipanteReuniao> participantes = participanteReuniaoRepository.findByReuniao_Id(reuniao.getId());
            out.add(ReuniaoResponse.from(reuniao, participantes));
        }
        return out;
    }

    @Transactional
    public ReuniaoResponse criar(ReuniaoRequest req) {
        Reuniao reuniao = new Reuniao();
        apply(reuniao, req);
        reuniao.setCondominioId(tenantAccessor.condominioAtual());
        reuniao.setCriadoPor(usuarioAtualId());
        reuniao = reuniaoRepository.save(reuniao);
        List<ParticipanteReuniao> participantes = replaceParticipantes(reuniao, req.participantes());
        return ReuniaoResponse.from(reuniao, participantes);
    }

    @Transactional
    public ReuniaoResponse atualizar(UUID id, ReuniaoRequest req) {
        Reuniao reuniao = reuniaoRepository.findById(id)
                .filter(x -> x.getCondominioId().equals(tenantAccessor.condominioAtual()))
                .orElseThrow(() -> new EntityNotFoundException("Reuniao nao encontrada."));
        apply(reuniao, req);
        reuniao = reuniaoRepository.save(reuniao);
        List<ParticipanteReuniao> participantes = replaceParticipantes(reuniao, req.participantes());
        return ReuniaoResponse.from(reuniao, participantes);
    }

    @Transactional
    public void deletar(UUID id) {
        Reuniao reuniao = reuniaoRepository.findById(id)
                .filter(x -> x.getCondominioId().equals(tenantAccessor.condominioAtual()))
                .orElseThrow(() -> new EntityNotFoundException("Reuniao nao encontrada."));
        participanteReuniaoRepository.deleteByReuniao_Id(reuniao.getId());
        reuniaoRepository.delete(reuniao);
    }

    private List<ParticipanteReuniao> replaceParticipantes(Reuniao reuniao, List<ReuniaoRequest.ParticipanteRequest> participantes) {
        participanteReuniaoRepository.deleteByReuniao_Id(reuniao.getId());
        if (participantes == null || participantes.isEmpty()) {
            return List.of();
        }
        List<ParticipanteReuniao> entities = new ArrayList<>();
        for (ReuniaoRequest.ParticipanteRequest p : participantes) {
            ParticipanteReuniao pr = new ParticipanteReuniao();
            pr.setReuniao(reuniao);
            pr.setNome(p.nome().trim());
            pr.setCargo(blankToNull(p.cargo()));
            pr.setPresente(p.presente() == null || p.presente());
            entities.add(pr);
        }
        return participanteReuniaoRepository.saveAll(entities);
    }

    private static void apply(Reuniao reuniao, ReuniaoRequest req) {
        reuniao.setTitulo(req.titulo().trim());
        reuniao.setTipo(req.tipo());
        reuniao.setDataHora(req.dataHora());
        reuniao.setLocal(blankToNull(req.local()));
        reuniao.setLink(blankToNull(req.link()));
        reuniao.setPauta(blankToNull(req.pauta()));
        reuniao.setResumo(blankToNull(req.resumo()));
        reuniao.setDecisoes(blankToNull(req.decisoes()));
        reuniao.setPendenciasGeradas(blankToNull(req.pendenciasGeradas()));
    }

    private static String blankToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private UUID usuarioAtualId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UsuarioTenantPrincipal principal) {
            return principal.getUsuarioId();
        }
        throw new IllegalStateException("Nao foi possivel identificar usuario autenticado");
    }
}
