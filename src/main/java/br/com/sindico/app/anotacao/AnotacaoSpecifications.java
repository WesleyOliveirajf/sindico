package br.com.sindico.app.anotacao;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;

/**
 * Predicados dinamicos para consulta de Anotacao via JPA Criteria API.
 *
 * Usar Specifications em vez de JPQL com parametros nulos evita o
 * PSQLException "could not determine data type of parameter" que ocorre
 * no Hibernate 6 + PostgreSQL quando um parametro String ou LocalDateTime
 * e passado como null em clausulas IS NULL do JPQL.
 */
public final class AnotacaoSpecifications {

    private AnotacaoSpecifications() {}

    public static Specification<Anotacao> doCondominio(UUID condominioId) {
        return (root, query, cb) -> cb.equal(root.get("condominioId"), condominioId);
    }

    /**
     * Filtra pelo texto em titulo, categoria, descricao e referencia (case-insensitive).
     * Chamado somente quando texto nao e nulo/vazio.
     */
    public static Specification<Anotacao> comTexto(String texto) {
        return (root, query, cb) -> {
            String pattern = "%" + texto.toLowerCase() + "%";
            return cb.or(
                    like(cb, root, "titulo", pattern),
                    likeCoalesce(cb, root, "categoria", pattern),
                    likeCoalesce(cb, root, "descricao", pattern),
                    likeCoalesce(cb, root, "referencia", pattern)
            );
        };
    }

    public static Specification<Anotacao> aPartirDe(LocalDateTime inicio) {
        return (root, query, cb) ->
                cb.greaterThanOrEqualTo(root.get("createdAt"), inicio);
    }

    public static Specification<Anotacao> ate(LocalDateTime fim) {
        return (root, query, cb) ->
                cb.lessThanOrEqualTo(root.get("createdAt"), fim);
    }

    // --- helpers ---

    private static Predicate like(CriteriaBuilder cb, Root<Anotacao> root,
                                   String field, String pattern) {
        return cb.like(cb.lower(root.get(field)), pattern);
    }

    private static Predicate likeCoalesce(CriteriaBuilder cb, Root<Anotacao> root,
                                           String field, String pattern) {
        return cb.like(
                cb.lower(cb.coalesce(root.get(field), "")),
                pattern
        );
    }
}
