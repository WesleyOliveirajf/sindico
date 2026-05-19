package br.com.sindico.app.senha;

import br.com.sindico.app.email.EmailService;
import br.com.sindico.app.usuario.SenhaResetToken;
import br.com.sindico.app.usuario.SenhaResetTokenRepository;
import br.com.sindico.app.usuario.Usuario;
import br.com.sindico.app.usuario.UsuarioRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Locale;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SenhaResetService {

    private static final int TOKEN_BYTES = 32; // 256 bits → 43 chars Base64url
    private static final int EXPIRY_MINUTES = 60;

    private final UsuarioRepository usuarioRepository;
    private final SenhaResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    public SenhaResetService(
            UsuarioRepository usuarioRepository,
            SenhaResetTokenRepository tokenRepository,
            PasswordEncoder passwordEncoder,
            EmailService emailService) {
        this.usuarioRepository = usuarioRepository;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    /**
     * Solicita reset. Sempre retorna sem revelar se o email existe (anti-enumeracao).
     *
     * @param email       email informado pelo usuario
     * @param baseUrl     URL base da aplicacao (ex: https://app.sindico.com)
     */
    @Transactional
    public void solicitarReset(String email, String baseUrl) {
        String emailNorm = email.trim().toLowerCase(Locale.ROOT);

        usuarioRepository.findAtivoPorEmailNormalizado(emailNorm).ifPresent(usuario -> {
            // Invalida tokens anteriores nao usados
            tokenRepository.invalidarTokensDoUsuario(usuario.getId());

            // Gera token seguro
            String rawToken = gerarToken();
            SenhaResetToken resetToken = new SenhaResetToken();
            resetToken.setUsuario(usuario);
            resetToken.setToken(hashToken(rawToken));
            resetToken.setExpiraEm(LocalDateTime.now().plusMinutes(EXPIRY_MINUTES));
            tokenRepository.save(resetToken);

            String link = baseUrl + "/redefinir-senha?token=" + rawToken;
            emailService.enviarResetSenha(usuario.getEmail(), usuario.getNome(), link);
        });
    }

    /**
     * Valida token e retorna usuario. Lanca {@link IllegalArgumentException} se invalido/expirado/usado.
     */
    @Transactional(readOnly = true)
    public Usuario validarToken(String token) {
        SenhaResetToken resetToken = tokenRepository.findByToken(hashToken(token))
                .orElseThrow(() -> new IllegalArgumentException("Link invalido ou expirado."));

        if (!resetToken.isValido()) {
            throw new IllegalArgumentException("Link invalido ou expirado. Solicite um novo.");
        }
        return resetToken.getUsuario();
    }

    /**
     * Redefine a senha. Marca token como usado atomicamente.
     */
    @Transactional
    public void redefinirSenha(String token, String novaSenha, String confirmarSenha) {
        SenhaResetToken resetToken = tokenRepository.findByToken(hashToken(token))
                .orElseThrow(() -> new IllegalArgumentException("Link invalido ou expirado."));

        if (!resetToken.isValido()) {
            throw new IllegalArgumentException("Link invalido ou expirado. Solicite um novo.");
        }

        validarNovaSenha(novaSenha, confirmarSenha);

        Usuario usuario = resetToken.getUsuario();
        usuario.setSenhaHash(passwordEncoder.encode(novaSenha));
        usuarioRepository.save(usuario);

        resetToken.setUsado(true);
        tokenRepository.save(resetToken);
    }

    private static void validarNovaSenha(String senha, String confirmar) {
        if (senha == null || senha.length() < 8) {
            throw new IllegalArgumentException("Senha deve ter no minimo 8 caracteres.");
        }
        if (!senha.equals(confirmar)) {
            throw new IllegalArgumentException("As senhas nao conferem.");
        }
        boolean temLetra = senha.chars().anyMatch(Character::isLetter);
        boolean temNumero = senha.chars().anyMatch(Character::isDigit);
        if (!temLetra || !temNumero) {
            throw new IllegalArgumentException("Senha deve conter letras e numeros.");
        }
    }

    private static String gerarToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String hashToken(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return "";
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 nao disponivel", e);
        }
    }
}
