package br.com.aweb.sistema_vendas.security;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import br.com.aweb.sistema_vendas.model.ResumoVendas;
import br.com.aweb.sistema_vendas.model.Usuario;
import br.com.aweb.sistema_vendas.model.UsuarioRole;
import br.com.aweb.sistema_vendas.repository.ClienteRepository;
import br.com.aweb.sistema_vendas.repository.ProdutoRepository;
import br.com.aweb.sistema_vendas.service.ClienteService;
import br.com.aweb.sistema_vendas.service.PedidoService;
import br.com.aweb.sistema_vendas.service.RelatorioPedidoService;
import br.com.aweb.sistema_vendas.service.ProdutoService;
import br.com.aweb.sistema_vendas.service.UsuarioService;

@WebMvcTest
@Import(ConfiguracoesDeSeguranca.class)
class AcessoPorRoleTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean ProdutoService produtoService;
    @MockitoBean ClienteService clienteService;
    @MockitoBean PedidoService pedidoService;
    @MockitoBean RelatorioPedidoService relatorioService;
    @MockitoBean ClienteRepository clienteRepository;
    @MockitoBean ProdutoRepository produtoRepository;
    @MockitoBean UsuarioService usuarioService;

    @BeforeEach
    void prepararMocks() {
        given(relatorioService.resumir(any())).willReturn(ResumoVendas.vazio());
        given(relatorioService.gerarCsv(any())).willReturn("﻿Pedido;Data\r\n");
    }

    @Test
    void relatorioEstaDisponivelParaAdminEPadrao() throws Exception {
        mvc.perform(get("/pedidos/relatorio").with(user(usuario(UsuarioRole.ADMIN))))
                .andExpect(status().isOk());
        mvc.perform(get("/pedidos/relatorio").with(user(usuario(UsuarioRole.PADRAO))))
                .andExpect(status().isOk());
    }

    @Test
    void relatorioExigeLoginEExportaCsv() throws Exception {
        mvc.perform(get("/pedidos/relatorio"))
                .andExpect(status().is3xxRedirection());
        mvc.perform(get("/pedidos/relatorio/csv"))
                .andExpect(status().is3xxRedirection());

        mvc.perform(get("/pedidos/relatorio/csv").with(user(usuario(UsuarioRole.PADRAO))))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/csv"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .header().string("Content-Disposition",
                                org.hamcrest.Matchers.containsString("relatorio-pedidos-")));
    }

    @Test
    void relatorioComPeriodoInvertidoMostraErro() throws Exception {
        mvc.perform(get("/pedidos/relatorio?inicio=2026-10-10&fim=2026-10-01")
                        .with(user(usuario(UsuarioRole.ADMIN))))
                .andExpect(status().isOk())
                .andExpect(content().string(
                        org.hamcrest.Matchers.containsString("posterior")));
    }

    @Test
    void logoETemaSaoPublicos() throws Exception {
        mvc.perform(get("/img/logo.svg")).andExpect(status().isOk());
        mvc.perform(get("/js/theme.js")).andExpect(status().isOk());
    }

    private Usuario usuario(UsuarioRole role) {
        return new Usuario("Teste", "teste@vendas.com", "hash", role);
    }

    @Test
    void anonimoEhRedirecionadoParaLogin() throws Exception {
        mvc.perform(get("/pedidos"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
        mvc.perform(get("/produtos"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    void paginaDeLoginEhPublica() throws Exception {
        mvc.perform(get("/login")).andExpect(status().isOk());
    }

    @Test
    void adminAcessaTudo() throws Exception {
        var admin = user(usuario(UsuarioRole.ADMIN));
        mvc.perform(get("/pedidos").with(admin)).andExpect(status().isOk());
        mvc.perform(get("/produtos").with(admin)).andExpect(status().isOk());
        mvc.perform(get("/produtos/novo").with(admin)).andExpect(status().isOk());
        mvc.perform(get("/clientes").with(admin)).andExpect(status().isOk());
        mvc.perform(get("/clientes/novo").with(admin)).andExpect(status().isOk());
    }

    @Test
    void padraoAcessaPedidosMasNaoProdutosNemClientes() throws Exception {
        var padrao = user(usuario(UsuarioRole.PADRAO));
        mvc.perform(get("/pedidos").with(padrao)).andExpect(status().isOk());
        mvc.perform(get("/produtos").with(padrao)).andExpect(status().isForbidden());
        mvc.perform(get("/produtos/novo").with(padrao)).andExpect(status().isForbidden());
        mvc.perform(post("/produtos/delete/1").with(padrao).with(csrf())).andExpect(status().isForbidden());
        mvc.perform(get("/clientes").with(padrao)).andExpect(status().isForbidden());
        mvc.perform(get("/clientes/edit/1").with(padrao)).andExpect(status().isForbidden());
    }

    @Test
    void paginaDeLogoutExigeLoginEPostEncerraSessao() throws Exception {
        mvc.perform(get("/logout").with(user(usuario(UsuarioRole.PADRAO))))
                .andExpect(status().isOk());
        mvc.perform(post("/logout").with(user(usuario(UsuarioRole.PADRAO))).with(csrf()))
                .andExpect(redirectedUrl("/login?logout"));
    }
}
