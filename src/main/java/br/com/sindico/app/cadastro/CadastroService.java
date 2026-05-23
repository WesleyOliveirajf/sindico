package br.com.sindico.app.cadastro;

import br.com.sindico.app.condominio.Condominio;
import br.com.sindico.app.condominio.CondominioRepository;
import br.com.sindico.app.security.PasswordPolicy;
import br.com.sindico.app.usuario.Usuario;
import br.com.sindico.app.usuario.UsuarioCondominio;
import br.com.sindico.app.usuario.UsuarioCondominioRepository;
import br.com.sindico.app.usuario.UsuarioRepository;
import br.com.sindico.app.usuario.UsuarioConsentimento;
import br.com.sindico.app.usuario.UsuarioConsentimentoRepository;
import java.util.Locale;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CadastroService {

    private final UsuarioRepository usuarioRepository;
    private final CondominioRepository condominioRepository;
    private final UsuarioCondominioRepository usuarioCondominioRepository;
    private final UsuarioConsentimentoRepository usuarioConsentimentoRepository;
    private final PasswordEncoder passwordEncoder;

    public CadastroService(
            UsuarioRepository usuarioRepository,
            CondominioRepository condominioRepository,
            UsuarioCondominioRepository usuarioCondominioRepository,
            UsuarioConsentimentoRepository usuarioConsentimentoRepository,
            PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.condominioRepository = condominioRepository;
        this.usuarioCondominioRepository = usuarioCondominioRepository;
        this.usuarioConsentimentoRepository = usuarioConsentimentoRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Cria usuario + condominio + vinculo SINDICO + consentimento LGPD em uma unica transacao.
     * Qualquer falha faz rollback completo.
     */
    @Transactional
    public void cadastrar(CadastroForm form, String ipAddress, String userAgent, String origem) {
        String nome = textoObrigatorio(form.getNome(), "Informe seu nome");
        String emailNorm = textoObrigatorio(form.getEmail(), "Informe o e-mail").toLowerCase(Locale.ROOT);
        String nomeCondominio = textoObrigatorio(form.getNomeCondominio(), "Informe o nome do condominio");

        if (!form.isAceitouTermos()) {
            throw new IllegalArgumentException("Você precisa aceitar os Termos de Uso e a Política de Privacidade.");
        }

        if (usuarioRepository.existsByEmailNormalizado(emailNorm)) {
            throw new IllegalArgumentException("Ja existe uma conta com este e-mail.");
        }

        PasswordPolicy.validateNewPassword(form.getSenha(), form.getConfirmarSenha());

        // 1. Cria usuario
        Usuario usuario = new Usuario();
        usuario.setNome(nome);
        usuario.setEmail(emailNorm);
        usuario.setSenhaHash(passwordEncoder.encode(form.getSenha()));
        // Novos cadastros ficam pendentes ate aprovacao pelo admin
        usuario.setStatus("pendente");
        usuario = usuarioRepository.save(usuario);

        // 2. Cria condominio
        Condominio condominio = new Condominio();
        condominio.setNome(nomeCondominio);
        condominio = condominioRepository.save(condominio);

        // 3. Vincula usuario ao condominio como SINDICO
        UsuarioCondominio vinculo = new UsuarioCondominio();
        vinculo.setUsuario(usuario);
        vinculo.setCondominio(condominio);
        vinculo.setPerfil("SINDICO");
        usuarioCondominioRepository.save(vinculo);

        // 4. Registra histórico de consentimento LGPD
        UsuarioConsentimento consentimento = new UsuarioConsentimento();
        consentimento.setUsuario(usuario);
        consentimento.setTermsVersion("v1.0");
        consentimento.setPrivacyPolicyVersion("v1.0");
        consentimento.setIpAddress(ipAddress);
        consentimento.setUserAgent(userAgent);
        consentimento.setMarketingConsent(form.isAceitouMarketing());
        consentimento.setOrigem(origem);
        usuarioConsentimentoRepository.save(consentimento);
    }

    private static String textoObrigatorio(String valor, String mensagem) {
        if (valor == null) {
            throw new IllegalArgumentException(mensagem);
        }
        String limpo = valor.trim();
        if (limpo.isEmpty()) {
            throw new IllegalArgumentException(mensagem);
        }
        return limpo;
    }

}
