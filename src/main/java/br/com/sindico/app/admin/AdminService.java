package br.com.sindico.app.admin;

import br.com.sindico.app.condominio.CondominioRepository;
import br.com.sindico.app.usuario.Usuario;
import br.com.sindico.app.usuario.UsuarioCondominio;
import br.com.sindico.app.usuario.UsuarioCondominioRepository;
import br.com.sindico.app.usuario.UsuarioRepository;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Casos de uso administrativos: metricas e gestao de status de usuarios. */
@Service
public class AdminService {

    private static final String STATUS_ATIVO = "ativo";
    private static final String STATUS_INATIVO = "inativo";
    private static final String STATUS_PENDENTE = "pendente";
    private static final long JANELA_ONLINE_MIN = 15;

    private final UsuarioRepository usuarioRepository;
    private final UsuarioCondominioRepository usuarioCondominioRepository;
    private final CondominioRepository condominioRepository;

    public AdminService(
            UsuarioRepository usuarioRepository,
            UsuarioCondominioRepository usuarioCondominioRepository,
            CondominioRepository condominioRepository) {
        this.usuarioRepository = usuarioRepository;
        this.usuarioCondominioRepository = usuarioCondominioRepository;
        this.condominioRepository = condominioRepository;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> estatisticas() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalUsuarios", usuarioRepository.count());
        stats.put("pendentes", usuarioRepository.countByStatus(STATUS_PENDENTE));
        stats.put("ativos", usuarioRepository.countByStatus(STATUS_ATIVO));
        stats.put("inativos", usuarioRepository.countByStatus(STATUS_INATIVO));
        stats.put("totalCondominios", condominioRepository.count());
        stats.put("onlineAgora",
                usuarioRepository.countOnlineRecentemente(LocalDateTime.now().minusMinutes(JANELA_ONLINE_MIN)));
        return stats;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listarUsuarios() {
        List<Usuario> usuarios = usuarioRepository.findAllByOrderByCreatedAtDesc();

        // Uma unica query para todos os vinculos (evita N+1); mantem o primeiro em caso de varios condominios.
        List<UUID> ids = usuarios.stream().map(Usuario::getId).toList();
        Map<UUID, UsuarioCondominio> vinculoPorUsuario = ids.isEmpty()
                ? Map.of()
                : usuarioCondominioRepository.findByUsuarioIdsComCondominio(ids).stream()
                        .collect(Collectors.toMap(uc -> uc.getUsuario().getId(), uc -> uc, (first, ignored) -> first));

        return usuarios.stream().map(u -> {
            UsuarioCondominio vinculo = vinculoPorUsuario.get(u.getId());
            Map<String, Object> dto = new LinkedHashMap<>();
            dto.put("id", u.getId().toString());
            dto.put("nome", u.getNome());
            dto.put("email", u.getEmail());
            dto.put("status", u.getStatus());
            dto.put("condominioNome", vinculo != null ? vinculo.getCondominio().getNome() : "");
            dto.put("perfil", vinculo != null ? vinculo.getPerfil() : "");
            dto.put("createdAt", u.getCreatedAt());
            dto.put("ultimoAcesso", u.getUltimoAcesso());
            return dto;
        }).collect(Collectors.toList());
    }

    /** @return false se o usuario nao existe */
    @Transactional
    public boolean aprovar(UUID id) {
        return alterarStatus(id, STATUS_ATIVO);
    }

    /** @return false se o usuario nao existe */
    @Transactional
    public boolean rejeitar(UUID id) {
        return alterarStatus(id, STATUS_INATIVO);
    }

    /** @return false se o usuario nao existe */
    @Transactional
    public boolean reativar(UUID id) {
        return alterarStatus(id, STATUS_ATIVO);
    }

    private boolean alterarStatus(UUID id, String status) {
        return usuarioRepository.findById(id).map(u -> {
            u.setStatus(status);
            usuarioRepository.save(u);
            return true;
        }).orElse(false);
    }
}
