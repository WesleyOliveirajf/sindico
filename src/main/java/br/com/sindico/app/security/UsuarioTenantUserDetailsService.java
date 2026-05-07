package br.com.sindico.app.security;

import br.com.sindico.app.usuario.Usuario;
import br.com.sindico.app.usuario.UsuarioCondominio;
import br.com.sindico.app.usuario.UsuarioCondominioRepository;
import br.com.sindico.app.usuario.UsuarioRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UsuarioTenantUserDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;
    private final UsuarioCondominioRepository usuarioCondominioRepository;

    public UsuarioTenantUserDetailsService(
            UsuarioRepository usuarioRepository,
            UsuarioCondominioRepository usuarioCondominioRepository
    ) {
        this.usuarioRepository = usuarioRepository;
        this.usuarioCondominioRepository = usuarioCondominioRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        String emailNorm = normalizeEmail(username);

        Usuario usuario = usuarioRepository
                .findAtivoPorEmailNormalizado(emailNorm)
                .orElseThrow(() -> new UsernameNotFoundException("Credenciais invalidas"));

        if (usuario.getSenhaHash() == null || usuario.getSenhaHash().isBlank()) {
            throw new UsernameNotFoundException("Credenciais invalidas");
        }

        List<UsuarioCondominio> vinculos =
                usuarioCondominioRepository.findByUsuario_IdOrderByCondominio_NomeAsc(usuario.getId());
        if (vinculos.isEmpty()) {
            throw new UsernameNotFoundException("Usuario sem condominio associado");
        }

        UUID condominioPadraoId =
                Optional.ofNullable(vinculos.getFirst().getCondominioId()).orElseThrow(() -> new UsernameNotFoundException(
                        "Usuario sem condominio associado"));

        Set<UUID> todosCondominios =
                vinculos.stream().map(UsuarioCondominio::getCondominioId).collect(Collectors.toSet());

        Map<String, GrantedAuthority> roles = new LinkedHashMap<>();
        for (UsuarioCondominio vc : vinculos) {
            String perfil =
                    vc.getPerfil() == null || vc.getPerfil().isBlank()
                            ? "USUARIO"
                            : vc.getPerfil().trim().toUpperCase(Locale.ROOT);
            roles.put(perfil, new SimpleGrantedAuthority("ROLE_" + perfil));
        }

        return new UsuarioTenantPrincipal(
                usuario.getId(),
                condominioPadraoId,
                todosCondominios,
                usuario.getEmail(),
                usuario.getSenhaHash(),
                List.copyOf(roles.values()));
    }

    private static String normalizeEmail(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new UsernameNotFoundException("Credenciais invalidas");
        }
        return raw.trim().toLowerCase(Locale.ROOT);
    }
}
