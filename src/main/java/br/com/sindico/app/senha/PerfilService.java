package br.com.sindico.app.senha;

import br.com.sindico.app.security.PasswordPolicy;
import br.com.sindico.app.security.UsuarioTenantPrincipal;
import br.com.sindico.app.usuario.Usuario;
import br.com.sindico.app.usuario.UsuarioRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

@Service
public class PerfilService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public PerfilService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public Usuario usuarioAtual() {
        UUID id = principalAtual().getUsuarioId();
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("Usuario nao encontrado"));
    }

    @Transactional
    public void atualizarPerfil(String nome, String telefone) {
        Usuario u = usuarioAtual();
        if (nome == null || nome.isBlank()) throw new IllegalArgumentException("Nome e obrigatorio.");
        u.setNome(nome.trim());
        u.setTelefone(telefone == null || telefone.isBlank() ? null : telefone.trim());
        usuarioRepository.save(u);
    }

    @Transactional
    public void trocarSenha(String senhaAtual, String novaSenha, String confirmarSenha) {
        Usuario u = usuarioAtual();

        if (!passwordEncoder.matches(senhaAtual, u.getSenhaHash())) {
            throw new IllegalArgumentException("Senha atual incorreta.");
        }
        PasswordPolicy.validateNewPassword(novaSenha, confirmarSenha, "Nova senha deve ter no minimo 8 caracteres.");
        if (passwordEncoder.matches(novaSenha, u.getSenhaHash())) {
            throw new IllegalArgumentException("Nova senha deve ser diferente da atual.");
        }

        u.setSenhaHash(passwordEncoder.encode(novaSenha));
        usuarioRepository.save(u);
    }

    private static UsuarioTenantPrincipal principalAtual() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!(principal instanceof UsuarioTenantPrincipal p)) {
            throw new IllegalStateException("Principal nao e UsuarioTenantPrincipal");
        }
        return p;
    }
}
