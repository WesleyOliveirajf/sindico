package br.com.sindico.app.prestador;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collections;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class PrestadorHistoricoCodec {

    private static final TypeReference<List<PrestadorHistoricoItem>> HISTORY_TYPE = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper;

    public PrestadorHistoricoCodec(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String encode(List<PrestadorHistoricoItem> historicoItens, String fallbackHistorico) {
        if (historicoItens == null || historicoItens.isEmpty()) {
            return fallbackHistorico;
        }
        try {
            return objectMapper.writeValueAsString(historicoItens);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("Nao foi possivel salvar o historico de servicos.");
        }
    }

    public List<PrestadorHistoricoItem> decode(String historicoServicos) {
        if (historicoServicos == null || historicoServicos.isBlank()) {
            return Collections.emptyList();
        }

        String raw = historicoServicos.trim();
        if (!raw.startsWith("[")) {
            return Collections.emptyList();
        }

        try {
            List<PrestadorHistoricoItem> itens = objectMapper.readValue(raw, HISTORY_TYPE);
            return itens == null ? Collections.emptyList() : itens;
        } catch (JsonProcessingException ex) {
            return Collections.emptyList();
        }
    }
}
