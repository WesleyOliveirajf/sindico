package br.com.sindico.app.security;

import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Utilitários de segurança compartilhados entre os services.
 * Resolve violação DRY (MELHORIA-002): os métodos usuarioAtualId() e blankToNull()
 * estavam duplicados em ManutencaoService, GastoService e AnexoService.
 */
public final class SecurityUtils {

    private SecurityUtils() {}

    /**
     * Retorna o UUID do usuário autenticado no contexto atual.
     *
     * @throws IllegalStateException se não houver usuário autenticado
     */
    public static UUID usuarioAtualId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UsuarioTenantPrincipal principal) {
            return principal.getUsuarioId();
        }
        throw new IllegalStateException("Nao foi possivel identificar usuario autenticado");
    }

    /**
     * Converte string em branco (null ou só espaços) para null.
     */
    public static String blankToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}

