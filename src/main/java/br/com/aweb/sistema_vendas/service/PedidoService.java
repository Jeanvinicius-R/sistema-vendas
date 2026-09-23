package br.com.aweb.sistema_vendas.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import br.com.aweb.sistema_vendas.model.Cliente;
import br.com.aweb.sistema_vendas.model.ItemPedido;
import br.com.aweb.sistema_vendas.model.Pedido;
import br.com.aweb.sistema_vendas.model.Produto;
import br.com.aweb.sistema_vendas.model.StatusPedido;
import br.com.aweb.sistema_vendas.repository.ClienteRepository;
import br.com.aweb.sistema_vendas.repository.PedidoRepository;
import br.com.aweb.sistema_vendas.repository.ProdutoRepository;
import jakarta.transaction.Transactional;

@Service
public class PedidoService {

    @Autowired
    private PedidoRepository pedidoRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private ProdutoRepository produtoRepository;

    public List<Pedido> listarTodos() {
        return pedidoRepository.findAll();
    }

    public Optional<Pedido> buscarPorId(Long id) {
        return pedidoRepository.findById(id);
    }

    @Transactional
    public Pedido criar(Pedido pedido) {
        if (pedido.getItens() == null || pedido.getItens().isEmpty()) {
            throw new IllegalArgumentException("O pedido deve conter ao menos um produto.");
        }

        Cliente cliente = clienteRepository.findById(pedido.getCliente().getId())
                .orElseThrow(() -> new IllegalArgumentException("Cliente não encontrado."));

        BigDecimal valorTotal = BigDecimal.ZERO;

        for (ItemPedido item : pedido.getItens()) {
            Produto produto = produtoRepository.findById(item.getProduto().getId())
                    .orElseThrow(() -> new IllegalArgumentException("Produto não encontrado."));

            if (produto.getQuantidadeEmEstoque() < item.getQuantidade()) {
                throw new IllegalArgumentException("Estoque insuficiente para o produto: " + produto.getNome());
            }

            produto.setQuantidadeEmEstoque(produto.getQuantidadeEmEstoque() - item.getQuantidade());
            produtoRepository.save(produto);

            item.setProduto(produto);
            item.setPedido(pedido);
            item.setPrecoUnitario(produto.getPreco());

            valorTotal = valorTotal.add(produto.getPreco().multiply(BigDecimal.valueOf(item.getQuantidade())));
        }

        pedido.setCliente(cliente);
        pedido.setStatus(StatusPedido.ATIVO);
        pedido.setDataPedido(LocalDateTime.now());
        pedido.setValorTotal(valorTotal);

        return pedidoRepository.save(pedido);
    }

    @Transactional
    public Pedido atualizar(Long id, Pedido pedidoAtualizado) {
        Pedido pedidoExistente = buscarPorId(id)
                .orElseThrow(() -> new IllegalArgumentException("Pedido não encontrado."));

        if (pedidoExistente.getStatus() != StatusPedido.ATIVO) {
            throw new IllegalStateException("Apenas pedidos ativos podem ser alterados.");
        }

        if (pedidoAtualizado.getItens() == null || pedidoAtualizado.getItens().isEmpty()) {
            throw new IllegalArgumentException("O pedido deve conter ao menos um produto.");
        }

        Cliente cliente = clienteRepository.findById(pedidoAtualizado.getCliente().getId())
                .orElseThrow(() -> new IllegalArgumentException("Cliente não encontrado."));

        // devolve ao estoque os itens atuais antes de aplicar a alteração
        devolverEstoque(pedidoExistente.getItens());
        pedidoExistente.getItens().clear();

        BigDecimal valorTotal = BigDecimal.ZERO;

        for (ItemPedido itemNovo : pedidoAtualizado.getItens()) {
            Produto produto = produtoRepository.findById(itemNovo.getProduto().getId())
                    .orElseThrow(() -> new IllegalArgumentException("Produto não encontrado."));

            if (produto.getQuantidadeEmEstoque() < itemNovo.getQuantidade()) {
                throw new IllegalArgumentException("Estoque insuficiente para o produto: " + produto.getNome());
            }

            produto.setQuantidadeEmEstoque(produto.getQuantidadeEmEstoque() - itemNovo.getQuantidade());
            produtoRepository.save(produto);

            ItemPedido item = new ItemPedido();
            item.setPedido(pedidoExistente);
            item.setProduto(produto);
            item.setQuantidade(itemNovo.getQuantidade());
            item.setPrecoUnitario(produto.getPreco());
            pedidoExistente.getItens().add(item);

            valorTotal = valorTotal.add(produto.getPreco().multiply(BigDecimal.valueOf(itemNovo.getQuantidade())));
        }

        pedidoExistente.setCliente(cliente);
        pedidoExistente.setValorTotal(valorTotal);

        return pedidoRepository.save(pedidoExistente);
    }

    @Transactional
    public void cancelar(Long id) {
        Pedido pedido = buscarPorId(id)
                .orElseThrow(() -> new IllegalArgumentException("Pedido não encontrado."));

        if (pedido.getStatus() == StatusPedido.CANCELADO) {
            throw new IllegalStateException("Este pedido já está cancelado.");
        }

        devolverEstoque(pedido.getItens());
        pedido.setStatus(StatusPedido.CANCELADO);
        pedidoRepository.save(pedido);
    }

    private void devolverEstoque(List<ItemPedido> itens) {
        for (ItemPedido item : itens) {
            Produto produto = item.getProduto();
            produto.setQuantidadeEmEstoque(produto.getQuantidadeEmEstoque() + item.getQuantidade());
            produtoRepository.save(produto);
        }
    }

}