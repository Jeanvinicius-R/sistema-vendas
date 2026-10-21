package br.com.aweb.sistema_vendas.model;

import java.math.BigDecimal;

import lombok.Value;

// Totais de um conjunto de pedidos. Só pedidos ATIVO contam como venda realizada.
@Value
public class ResumoVendas {

    BigDecimal totalVendas;
    long pedidosAtivos;
    long itensVendidos;
    BigDecimal ticketMedio;
    long pedidosCancelados;
    BigDecimal valorCancelado;

    public static ResumoVendas vazio() {
        return new ResumoVendas(
            BigDecimal.ZERO, 0, 0, BigDecimal.ZERO, 0, BigDecimal.ZERO
        );
    }
}
