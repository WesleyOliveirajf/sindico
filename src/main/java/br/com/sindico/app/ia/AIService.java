package br.com.sindico.app.ia;

import br.com.sindico.app.gasto.Gasto;
import br.com.sindico.app.gasto.GastoRepository;
import br.com.sindico.app.manutencao.Manutencao;
import br.com.sindico.app.manutencao.ManutencaoRepository;
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

    public AIService(
            LLMClientFactory llmClientFactory,
            TenantAccessor tenantAccessor,
            ReuniaoRepository reuniaoRepository,
            ParticipanteReuniaoRepository participanteReuniaoRepository,
            GastoRepository gastoRepository,
            ManutencaoRepository manutencaoRepository) {
        this.llmClientFactory = llmClientFactory;
        this.tenantAccessor = tenantAccessor;
        this.reuniaoRepository = reuniaoRepository;
        this.participanteReuniaoRepository = participanteReuniaoRepository;
        this.gastoRepository = gastoRepository;
        this.manutencaoRepository = manutencaoRepository;
    }

    @Transactional(readOnly = true)
    public String chat(String mensagem) {
        LLMClient client = llmClientFactory.criar();
        return client.chat(SYSTEM_SINDICO, mensagem);
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

    private static String orNA(String s) {
        return (s == null || s.isBlank()) ? "Nao informado" : s;
    }
}