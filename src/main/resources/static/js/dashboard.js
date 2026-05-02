(function () {
    var form = document.querySelector("[data-submit-once]");
    if (!form) {
        return;
    }

    var button = form.querySelector("[data-submit-btn]");
    if (!button) {
        return;
    }

    var inicioInput = form.querySelector("#inicioEm");
    var fimInput = form.querySelector("#fimEm");
    var clientError = document.querySelector("#client-error");

    function showError(message) {
        if (!clientError) {
            return;
        }

        clientError.textContent = message;
        clientError.classList.remove("hidden");
    }

    function clearError() {
        if (!clientError) {
            return;
        }

        clientError.textContent = "";
        clientError.classList.add("hidden");
    }

    form.addEventListener("submit", function (event) {
        clearError();

        if (inicioInput && fimInput && inicioInput.value && fimInput.value) {
            var inicio = new Date(inicioInput.value);
            var fim = new Date(fimInput.value);
            if (fim <= inicio) {
                event.preventDefault();
                showError("A data de fim deve ser maior que a data de inicio.");
                fimInput.focus();
                return;
            }
        }

        button.disabled = true;
        button.textContent = "Salvando...";
    });

    if (inicioInput) {
        inicioInput.addEventListener("input", clearError);
    }

    if (fimInput) {
        fimInput.addEventListener("input", clearError);
    }
})();
