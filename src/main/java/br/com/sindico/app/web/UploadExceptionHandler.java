package br.com.sindico.app.web;

import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

/**
 * Upload acima do limite do servlet. Fica fora do {@link GlobalApiExceptionHandler} porque o
 * multipart e resolvido antes de o controller ser escolhido: advices com seletor (ex.:
 * {@code annotations = RestController.class}) nao se aplicam quando ainda nao ha handler.
 */
@RestControllerAdvice
public class UploadExceptionHandler {

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    @ResponseStatus(HttpStatus.PAYLOAD_TOO_LARGE)
    public Map<String, Object> handleUploadGrande(@SuppressWarnings("unused") MaxUploadSizeExceededException ex) {
        return Map.of("message", "Arquivo excede tamanho maximo permitido", "status", 413);
    }
}
