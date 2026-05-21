package br.com.sindico.app.ia;

import br.com.sindico.app.anotacao.Anotacao;
import br.com.sindico.app.anotacao.AnotacaoRepository;
import br.com.sindico.app.compromisso.Compromisso;
import br.com.sindico.app.compromisso.CompromissoRepository;
import br.com.sindico.app.gasto.Gasto;
import br.com.sindico.app.gasto.GastoRepository;
import br.com.sindico.app.manutencao.Manutencao;
import br.com.sindico.app.manutencao.ManutencaoRepository;
import br.com.sindico.app.morador.Morador;
import br.com.sindico.app.morador.MoradorRepository;
import br.com.sindico.app.morador.UnidadeRepository;
import br.com.sindico.app.prestador.PrestadorServico;
import br.com.sindico.app.prestador.PrestadorServicoRepository;
import br.com.sindico.app.reuniao.ParticipanteReuniao;
import br.com.sindico.app.reuniao.ParticipanteReuniaoRepository;
import br.com.sindico.app.reuniao.Reuniao;
import br.com.sindico.app.reuniao.ReuniaoRepository;
import br.com.sindico.app.security.TenantAccessor;
import jakarta.persistence.EntityNotFoundException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AIService {

    private static final String SYSTEM_SINDICO =
            "Voce e um assistente especialista em gestao de condominios no Brasil. " +
            "Responda sempre em portugues, de forma clara, objetiva e profissional. " +
            "Use linguagem adequada para um sindico.";

    private final LLMClientFactory llmClientFactory;
    private final TenantAccessor tenantAccessor;
    private final ReuniaoRepository reuniaoRepository;
    private final ParticipanteReuniaoRepository participanteReuniaoRepository;
    private final GastoRepository gastoRepository;
    private final ManutencaoRepository manutencaoRepository;
    private final MoradorRepository moradorRepository;
    private final UnidadeRepository unidadeRepository;
    private final PrestadorServicoRepository prestadorServicoRepository;
    private final AnotacaoRepository anotacaoRepository;
    private final CompromissoRepository compromissoRepository;

    public AIService(
            LLMClientFactory llmClientFactory,
            TenantAccessor tenantAccessor,
            ReuniaoRepository reuniaoRepository,
            ParticipanteReuniaoRepository participanteReuniaoRepository,
            GastoRepository gastoRepository,
            ManutencaoRepository manutencaoRepository,
            MoradorRepository moradorRepository,
            UnidadeRepository unidadeRepository,
            PrestadorServicoRepository prestadorServicoRepository,
            AnotacaoRepository anotacaoRepository,
            CompromissoRepository compromissoRepository) {
        this.llmClientFactory = llmClientFactory;
        this.tenantAccessor = tenantAccessor;
        this.reuniaoRepository = reuniaoRepository;
        this.participanteReuniaoRepository = participanteReuniaoRepository;
        this.gastoRepository = gastoRepository;
        this.manutencaoRepository = manutencaoRepository;
        this.moradorRepository = moradorRepository;
        this.unidadeRepository = unidadeRepository;
        this.prestadorServicoRepository = prestadorServicoRepository;
        this.anotacaoRepository = anotacaoRepository;
        this.compromissoRepository = compromissoRepository;
    }

    @Transactional(readOnly = true)
    public String chat(String mensagem) {
        UUID condominioId = tenantAccessor.condominioAtual();
        String contexto = construirContextoCondominio(condominioId);

        String systemPrompt = SYSTEM_SINDICO + "\n\n" +
                "Abaixo estao as informacoes reais e atualizadas do condominio do cliente que voce gerencia. " +
                "Use estes dados para responder de forma precisa, objetiva e contextualizada as perguntas do usuario sobre o condominio dele.\n\n" +
                contexto;

        LLMClient client = llmClientFactory.criar();
        return client.chat(systemPrompt, mensagem);
    }

    @Transactional(readOnly = true)
    public String gerarAta(UUID reuniaoId) {
        UUID condominioId = tenantAccessor.condominioAtual();
        Reuniao reuniao = reuniaoRepository.findById(reuniaoId)
                .filter(r -> r.getCondominioId().equals(condominioId))
                .orElseThrow(() -> new EntityNotFoundException("Reuniao nao encontrada"));

        List<ParticipanteReuniao> participantes =
                participanteReuniaoRepository.findByReuniao_Id(reuniaoId);

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy 'as' HH:mm");
        String dataFormatada = reuniao.getDataHora() != null
                ? reuniao.getDataHora().format(fmt) : "Data nao informada";

        String presentes = participantes.stream()
                .filter(ParticipanteReuniao::isPresente)
                .map(p -> p.getNome() + (p.getCargo() != null ? " (" + p.getCargo() + ")" : ""))
                .collect(Collectors.joining(", "));

        String ausentes = participantes.stream()
                .filter(p -> !p.isPresente())
                .map(ParticipanteReuniao::getNome)
                .collect(Collectors.joining(", "));

        String contexto = String.format("""
                Dados da reuniao:
                - Titulo: %s
                - Tipo: %s
                - Data e hora: %s
                - Local: %s
                - Pauta: %s
                - Resumo/notas: %s
                - Decisoes tomadas: %s
                - Pendencias geradas: %s
                - Participantes presentes: %s
                - Participantes ausentes: %s
                """,
                reuniao.getTitulo(),
                reuniao.getTipo(),
                dataFormatada,
                orNA(reuniao.getLocal()),
                orNA(reuniao.getPauta()),
                orNA(reuniao.getResumo()),
                orNA(reuniao.getDecisoes()),
                orNA(reuniao.getPendenciasGeradas()),
                presentes.isBlank() ? "Nao informados" : presentes,
                ausentes.isBlank() ? "Nenhum" : ausentes
        );

        String prompt = "Com base nos dados abaixo, gere uma ata formal de reuniao de condominio, " +
                "incluindo abertura, ordem do dia, deliberacoes, encerramento e espaco para assinaturas.\n\n" + contexto;

        LLMClient client = llmClientFactory.criar();
        return client.chat(SYSTEM_SINDICO, prompt);
    }

    @Transactional(readOnly = true)
    public String analisarGastos(Integer mes, Integer ano) {
        List<Gasto> gastos = gastoRepository.findByCondominioIdOrderByDataGastoDesc(
                tenantAccessor.condominioAtual());

        List<Gasto> filtrados = gastos.stream()
                .filter(g -> mes == null || (g.getDataGasto() != null && g.getDataGasto().getMonthValue() == mes))
                .filter(g -> ano == null || (g.getDataGasto() != null && g.getDataGasto().getYear() == ano))
                .toList();

        if (filtrados.isEmpty()) {
            return "Nenhum gasto encontrado para o periodo informado.";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Lista de gastos:\n");
        for (Gasto g : filtrados) {
            sb.append(String.format("- %s | %s | R$ %.2f | %s | %s\n",
                    g.getDescricao(), g.getTipo(), g.getValor(),
                    g.getDataGasto(), g.isFixo() ? "Fixo" : "Variavel"));
        }

        String prompt = "Analise os gastos do condominio abaixo e forneça:\n" +
                "1. Resumo financeiro do periodo\n" +
                "2. Maiores categorias de gasto\n" +
                "3. Proporcao fixo vs variavel\n" +
                "4. Alertas ou anomalias identificados\n" +
                "5. Sugestoes de otimizacao de custos\n\n" + sb;

        LLMClient client = llmClientFactory.criar();
        return client.chat(SYSTEM_SINDICO, prompt);
    }

    @Transactional(readOnly = true)
    public TriagemResponse triarManutencao(String descricao) {
        List<Manutencao> historico = manutencaoRepository
                .findByCondominioIdOrderByCreatedAtDesc(tenantAccessor.condominioAtual())
                .stream().limit(20).toList();

        String historicoTexto = historico.stream()
                .map(m -> String.format("- %s | %s | %s | %s",
                        m.getTitulo(), m.getTipo(), m.getCategoria(), m.getStatus()))
                .collect(Collectors.joining("\n"));

        String prompt = String.format("""
                Voce deve analisar a seguinte descricao de problema em um condominio e retornar um JSON com os campos:
                - tipo: "PREVENTIVA" ou "CORRETIVA"
                - categoria: categoria da manutencao (ex: Hidraulica, Eletrica, Estrutural, Elevador, Portaria, Jardinagem, Pintura, etc)
                - urgencia: "BAIXA", "MEDIA" ou "ALTA"
                - titulaSugerido: titulo curto para a ordem de servico (max 80 chars)
                - observacoes: observacoes adicionais para o prestador

                Descricao do problema: %s

                Historico recente de manutencoes do condominio:
                %s

                Responda APENAS com o JSON, sem texto adicional, sem markdown.
                """, descricao, historicoTexto.isBlank() ? "Nenhum historico disponivel." : historicoTexto);

        LLMClient client = llmClientFactory.criar();
        String json = client.chat(
                "Voce e um especialista em manutencao predial. Responda somente com JSON valido.",
                prompt
        );

        return TriagemResponse.parseJson(json.trim());
    }

    private String construirContextoCondominio(UUID condominioId) {
        StringBuilder sb = new StringBuilder();
        sb.append("--- INFORMACOES REAIS E ATUALIZADAS DO CONDOMINIO ---\n\n");

        // 1. Unidades e Moradores
        sb.append("[MORADORES E UNIDADES CADASTRADAS]\n");
        List<Morador> moradores = moradorRepository.listarAtivosPorCondominio(condominioId);
        if (moradores.isEmpty()) {
            sb.append("Nenhum morador ativo cadastrado.\n");
        } else {
            for (Morador m : moradores) {
                String unidadeRotulo = m.getUnidade() != null ? m.getUnidade().getRotulo() : "Nao associada";
                sb.append(String.format("- Nome: %s | Unidade: %s | Papel: %s | Telefone: %s | Email: %s\n",
                        m.getNome(), unidadeRotulo, m.getPapel(), orNA(m.getTelefone()), orNA(m.getEmail())));
            }
        }
        sb.append("\n");

        // 2. Manutenções
        sb.append("[HISTORICO DE MANUTENCOES (Ultimas 30)]\n");
        List<Manutencao> manutencoes = manutencaoRepository.findByCondominioIdOrderByCreatedAtDesc(condominioId)
                .stream().limit(30).toList();
        if (manutencoes.isEmpty()) {
            sb.append("Nenhuma manutencao cadastrada.\n");
        } else {
            for (Manutencao m : manutencoes) {
                sb.append(String.format("- Titulo: %s | Tipo: %s | Categoria: %s | Status: %s | Prioridade: %s | Local: %s | Custo Realizado: %s\n",
                        m.getTitulo(), m.getTipo(), orNA(m.getCategoria()), m.getStatus(), orNA(m.getPrioridade()), orNA(m.getLocal()),
                        m.getCustoRealizado() != null ? "R$ " + m.getCustoRealizado() : "Nao informado"));
            }
        }
        sb.append("\n");

        // 3. Reuniões
        sb.append("[REUNIOES E ASSEMBLEIAS (Ultimas 15)]\n");
        List<Reuniao> reunioes = reuniaoRepository.findByCondominioIdOrderByDataHoraDesc(condominioId)
                .stream().limit(15).toList();
        if (reunioes.isEmpty()) {
            sb.append("Nenhuma reuniao cadastrada.\n");
        } else {
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            for (Reuniao r : reunioes) {
                String dataStr = r.getDataHora() != null ? r.getDataHora().format(fmt) : "Nao informada";
                sb.append(String.format("- Titulo: %s | Tipo: %s | Data/Hora: %s | Pauta: %s | Decisoes: %s | Pendencias: %s\n",
                        r.getTitulo(), r.getTipo(), dataStr, orNA(r.getPauta()), orNA(r.getDecisoes()), orNA(r.getPendenciasGeradas())));
            }
        }
        sb.append("\n");

        // 4. Gastos
        sb.append("[HISTORICO DE GASTOS (Ultimos 30)]\n");
        List<Gasto> gastos = gastoRepository.findByCondominioIdOrderByDataGastoDesc(condominioId)
                .stream().limit(30).toList();
        if (gastos.isEmpty()) {
            sb.append("Nenhum gasto cadastrado.\n");
        } else {
            for (Gasto g : gastos) {
                sb.append(String.format("- Descricao: %s | Tipo: %s | Valor: R$ %.2f | Data: %s | Recorrencia: %s\n",
                        g.getDescricao(), g.getTipo(), g.getValor(), g.getDataGasto(), g.isFixo() ? "Fixo" : "Variavel"));
            }
        }
        sb.append("\n");

        // 5. Compromissos
        sb.append("[COMPROMISSOS DA AGENDA (Ultimos 20)]\n");
        List<Compromisso> compromissos = compromissoRepository.findByCondominioIdOrderByInicioEmDesc(condominioId)
                .stream().limit(20).toList();
        if (compromissos.isEmpty()) {
            sb.append("Nenhum compromisso agendado.\n");
        } else {
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            for (Compromisso c : compromissos) {
                String inicioStr = c.getInicioEm() != null ? c.getInicioEm().format(fmt) : "Nao informada";
                sb.append(String.format("- Titulo: %s | Tipo: %s | Inicio: %s | Local: %s | Status: %s\n",
                        c.getTitulo(), c.getTipo(), inicioStr, orNA(c.getLocal()), c.getStatus()));
            }
        }
        sb.append("\n");

        // 6. Prestadores de Serviço
        sb.append("[PRESTADORES DE SERVICO ATIVOS]\n");
        List<PrestadorServico> prestadores = prestadorServicoRepository.findByCondominioIdAndAtivoTrueOrderByNomeAsc(condominioId);
        if (prestadores.isEmpty()) {
            sb.append("Nenhum prestador de servico ativo cadastrado.\n");
        } else {
            for (PrestadorServico p : prestadores) {
                sb.append(String.format("- Nome: %s | Telefone: %s | Historico/Servicos: %s\n",
                        p.getNome(), p.getTelefone(), orNA(p.getHistoricoServicos())));
            }
        }
        sb.append("\n");

        // 7. Anotações
        sb.append("[ANOTACOES E AVISOS (Ultimos 20)]\n");
        List<Anotacao> anotacoes = anotacaoRepository.findByCondominioIdOrderByCreatedAtDesc(condominioId)
                .stream().limit(20).toList();
        if (anotacoes.isEmpty()) {
            sb.append("Nenhuma anotacao cadastrada.\n");
        } else {
            for (Anotacao a : anotacoes) {
                sb.append(String.format("- Titulo: %s | Categoria: %s | Descricao: %s | Importancia: %s\n",
                        a.getTitulo(), orNA(a.getCategoria()), orNA(a.getDescricao()), a.getImportancia()));
            }
        }
        sb.append("\n----------------------------------------------");
        return sb.toString();
    }

    private static String orNA(String s) {
        return (s == null || s.isBlank()) ? "Nao informado" : s;
    }
}