/* Tema claro/escuro: carregado no <head> para aplicar antes da primeira pintura (sem "piscar"). */
(function () {
    var CHAVE = 'sv-tema';
    var raiz = document.documentElement;

    function lerSalvo() {
        try { return localStorage.getItem(CHAVE); } catch (e) { return null; }
    }

    function salvar(tema) {
        try { localStorage.setItem(CHAVE, tema); } catch (e) { /* navegação privada: ignora */ }
    }

    function preferidoDoSistema() {
        return window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches
            ? 'dark'
            : 'light';
    }

    function aplicar(tema) {
        raiz.setAttribute('data-bs-theme', tema);
    }

    aplicar(lerSalvo() || preferidoDoSistema());

    document.addEventListener('DOMContentLoaded', function () {
        document.querySelectorAll('[data-tema-toggle]').forEach(function (botao) {
            botao.addEventListener('click', function () {
                var novo = raiz.getAttribute('data-bs-theme') === 'dark' ? 'light' : 'dark';
                aplicar(novo);
                salvar(novo);
            });
        });
    });

    // Na impressão (PDF do relatório) sempre usa o tema claro, para economizar tinta e manter a leitura
    var antesDeImprimir = null;

    window.addEventListener('beforeprint', function () {
        antesDeImprimir = raiz.getAttribute('data-bs-theme');
        aplicar('light');
    });

    window.addEventListener('afterprint', function () {
        if (antesDeImprimir) { aplicar(antesDeImprimir); }
    });
})();
