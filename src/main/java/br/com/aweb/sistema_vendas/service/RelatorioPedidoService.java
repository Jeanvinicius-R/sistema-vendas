package br.com.aweb.sistema_vendas.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.aweb.sistema_vendas.model.ItemPedido;
import br.com.aweb.sistema_vendas.model.Pedido;
import br.com.aweb.sistema_vendas.model.ResumoVendas;
import br.com.aweb.sistema_vendas.model.StatusPedido;
import br.com.aweb.sistema_vendas.repository.PedidoRepository;

@Service
public class RelatorioPedidoService {

    private final PedidoRepository pedidoRepository;

    public RelatorioPedidoService(PedidoRepository pedidoRepository) {
        this.pedidoRepository = pedidoRepository;
    }

    // Todos os pedidos, do mais recente para o mais antigo.
    @Transactional(readOnly = true)
    public List<Pedido> listarTodos() {
        return pedidoRepository.findAll(Sort.by(Sort.Direction.DESC, "dataPedido"));
    }

    // Filtros opcionais; null significa "sem filtro". As duas datas são inclusivas.
    @Transactional(readOnly = true)
    public List<Pedido> filtrar(
            LocalDate inicio,
            LocalDate fim,
            StatusPedido status,
            Long clienteId) {

        LocalDateTime de = inicio != null ? inicio.atStartOfDay() : null;
        LocalDateTime ate = fim != null ? fim.plusDays(1).atStartOfDay() : null;

        return listarTodos().stream()
                .filter(p -> status == null || p.getStatus() == status)
                .filter(p -> clienteId == null
                        || p.getCliente().getId().equals(clienteId))
                .filter(p -> de == null || !p.getDataPedido().isBefore(de))
                .filter(p -> ate == null || p.getDataPedido().isBefore(ate))
                .toList();
    }

    // Só pedidos ATIVO somam como venda; cancelados são totalizados à parte.
    public ResumoVendas resumir(List<Pedido> pedidos) {

        List<Pedido> ativos = pedidos.stream()
                .filter(p -> p.getStatus() == StatusPedido.ATIVO)
                .toList();

        List<Pedido> cancelados = pedidos.stream()
                .filter(p -> p.getStatus() == StatusPedido.CANCELADO)
                .toList();

        BigDecimal total = somar(ativos);

        BigDecimal ticketMedio = ativos.isEmpty()
                ? BigDecimal.ZERO
                : total.divide(BigDecimal.valueOf(ativos.size()), 2, RoundingMode.HALF_UP);

        long itens = ativos.stream()
                .flatMap(p -> p.getItens().stream())
                .mapToLong(ItemPedido::getQuantidade)
                .sum();

        return new ResumoVendas(
            total,
            ativos.size(),
            itens,
            ticketMedio,
            cancelados.size(),
            somar(cancelados)
        );
    }

    // CSV para Excel em português: separador ";", decimal ",", UTF-8 com BOM.
    public String gerarCsv(List<Pedido> pedidos) {

        StringBuilder csv = new StringBuilder("﻿");

        csv.append("Pedido;Data;Cliente;CPF;Status;Produtos;Itens;Valor total (R$)\r\n");

        for (Pedido p : pedidos) {
            long itens = p.getItens().stream()
                    .mapToLong(ItemPedido::getQuantidade)
                    .sum();

            String produtos = p.getItens().stream()
                    .map(i -> i.getProduto().getNome() + " x" + i.getQuantidade())
                    .collect(Collectors.joining(", "));

            csv.append(String.join(";",
                celula(String.valueOf(p.getId())),
                celula(p.getDataPedidoFormatada()),
                celula(p.getCliente().getNomeCompleto()),
                celula(p.getCliente().getCpf()),
                celula(p.getStatus().name()),
                celula(produtos),
                celula(String.valueOf(itens)),
                celula(dinheiro(p.getValorTotal()))
            )).append("\r\n");
        }

        ResumoVendas resumo = resumir(pedidos);

        csv.append("\r\n");
        csv.append(";;;;;;")
           .append(celula("Total de vendas (pedidos ativos)")).append(';')
           .append(celula(dinheiro(resumo.getTotalVendas()))).append("\r\n");
        csv.append(";;;;;;")
           .append(celula("Cancelados (não somados)")).append(';')
           .append(celula(dinheiro(resumo.getValorCancelado()))).append("\r\n");

        return csv.toString();
    }

    private BigDecimal somar(List<Pedido> pedidos) {
        return pedidos.stream()
                .map(Pedido::getValorTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private String dinheiro(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_UP)
                .toPlainString()
                .replace('.', ',');
    }

    // Aspas duplicadas e prefixo "'" contra injeção de fórmula no Excel (=, +, -, @).
    private String celula(String valor) {
        String texto = valor == null ? "" : valor;
        if (!texto.isEmpty() && "=+-@\t\r".indexOf(texto.charAt(0)) >= 0) {
            texto = "'" + texto;
        }
        return "\"" + texto.replace("\"", "\"\"") + "\"";
    }
}
