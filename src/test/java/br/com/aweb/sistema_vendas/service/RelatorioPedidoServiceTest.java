package br.com.aweb.sistema_vendas.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;

import br.com.aweb.sistema_vendas.model.Cliente;
import br.com.aweb.sistema_vendas.model.ItemPedido;
import br.com.aweb.sistema_vendas.model.Pedido;
import br.com.aweb.sistema_vendas.model.Produto;
import br.com.aweb.sistema_vendas.model.ResumoVendas;
import br.com.aweb.sistema_vendas.model.StatusPedido;
import br.com.aweb.sistema_vendas.repository.PedidoRepository;

class RelatorioPedidoServiceTest {

    private RelatorioPedidoService service;
    private Cliente maria;
    private Cliente joao;

    private Pedido pedidoMaria10;      // ATIVO, 10/10, 2 x 50,00 = 100,00
    private Pedido pedidoJoao12;       // ATIVO, 12/10, 1 x 30,00 = 30,00
    private Pedido pedidoMaria15;      // CANCELADO, 15/10, 1 x 999,00

    @BeforeEach
    void preparar() {
        PedidoRepository repository = mock(PedidoRepository.class);
        service = new RelatorioPedidoService(repository);

        maria = cliente(1L, "Maria Souza", "529.982.247-25");
        joao = cliente(2L, "=CMD|' /C calc'!A0", "111.444.777-35");

        pedidoMaria10 = pedido(1L, maria, LocalDateTime.of(2026, 10, 10, 9, 0),
                StatusPedido.ATIVO, item("Teclado", "50.00", 2));
        pedidoJoao12 = pedido(2L, joao, LocalDateTime.of(2026, 10, 12, 23, 59),
                StatusPedido.ATIVO, item("Mouse", "30.00", 1));
        pedidoMaria15 = pedido(3L, maria, LocalDateTime.of(2026, 10, 15, 14, 30),
                StatusPedido.CANCELADO, item("Monitor", "999.00", 1));

        when(repository.findAll(any(Sort.class)))
                .thenReturn(List.of(pedidoMaria15, pedidoJoao12, pedidoMaria10));
    }

    @Test
    void totalDeVendasSomaSomentePedidosAtivos() {
        ResumoVendas resumo = service.resumir(service.listarTodos());

        assertEquals(0, new BigDecimal("130.00").compareTo(resumo.getTotalVendas()));
        assertEquals(2, resumo.getPedidosAtivos());
        assertEquals(3, resumo.getItensVendidos());
        assertEquals(0, new BigDecimal("65.00").compareTo(resumo.getTicketMedio()));
        assertEquals(1, resumo.getPedidosCancelados());
        assertEquals(0, new BigDecimal("999.00").compareTo(resumo.getValorCancelado()));
    }

    @Test
    void resumoDeListaVaziaEhZerado() {
        ResumoVendas resumo = service.resumir(List.of());

        assertEquals(0, BigDecimal.ZERO.compareTo(resumo.getTotalVendas()));
        assertEquals(0, BigDecimal.ZERO.compareTo(resumo.getTicketMedio()));
        assertEquals(0, resumo.getPedidosAtivos());
    }

    @Test
    void filtraPorPeriodoComDatasInclusivas() {
        List<Pedido> resultado = service.filtrar(
                LocalDate.of(2026, 10, 10), LocalDate.of(2026, 10, 12), null, null);

        // 12/10 às 23:59 ainda entra no dia final
        assertEquals(List.of(pedidoJoao12, pedidoMaria10), resultado);
    }

    @Test
    void filtraPorStatusECliente() {
        assertEquals(List.of(pedidoMaria15),
                service.filtrar(null, null, StatusPedido.CANCELADO, null));

        assertEquals(List.of(pedidoMaria15, pedidoMaria10),
                service.filtrar(null, null, null, 1L));

        assertTrue(service.filtrar(null, null, StatusPedido.CANCELADO, 2L).isEmpty());
    }

    @Test
    void semFiltrosRetornaTudoDoMaisRecenteParaOMaisAntigo() {
        assertEquals(3, service.filtrar(null, null, null, null).size());
    }

    @Test
    void csvTemCabecalhoLinhasETotais() {
        String csv = service.gerarCsv(List.of(pedidoMaria10, pedidoMaria15));

        assertTrue(csv.startsWith("﻿Pedido;Data;Cliente;CPF;Status;Produtos;Itens;Valor total (R$)"));
        assertTrue(csv.contains("\"Teclado x2\""));
        assertTrue(csv.contains("\"100,00\""));
        assertTrue(csv.contains("\"Total de vendas (pedidos ativos)\";\"100,00\""));
        assertTrue(csv.contains("\"Cancelados (não somados)\";\"999,00\""));
    }

    @Test
    void csvNeutralizaFormulasEEscapaAspas() {
        String csv = service.gerarCsv(List.of(pedidoJoao12));

        // célula começando com "=" ganha apóstrofo; aspas internas são dobradas
        assertTrue(csv.contains("\"'=CMD|' /C calc'!A0\""));
    }

    // ---- auxiliares ----

    private Cliente cliente(Long id, String nome, String cpf) {
        Cliente c = new Cliente();
        c.setId(id);
        c.setNomeCompleto(nome);
        c.setCpf(cpf);
        return c;
    }

    private ItemPedido item(String nomeProduto, String preco, int quantidade) {
        Produto produto = new Produto();
        produto.setNome(nomeProduto);
        produto.setPreco(new BigDecimal(preco));
        return new ItemPedido(produto, quantidade);
    }

    private Pedido pedido(Long id, Cliente cliente, LocalDateTime data, StatusPedido status, ItemPedido item) {
        Pedido p = new Pedido(cliente);
        p.setId(id);
        p.setDataPedido(data);
        p.setStatus(status);
        p.getItens().add(item);
        p.setValorTotal(item.getPrecoUnitario().multiply(BigDecimal.valueOf(item.getQuantidade())));
        return p;
    }
}
