package br.com.aweb.sistema_vendas.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.aweb.sistema_vendas.model.Cliente;
import br.com.aweb.sistema_vendas.model.ItemPedido;
import br.com.aweb.sistema_vendas.model.Pedido;
import br.com.aweb.sistema_vendas.model.Produto;
import br.com.aweb.sistema_vendas.model.StatusPedido;
import br.com.aweb.sistema_vendas.repository.ClienteRepository;
import br.com.aweb.sistema_vendas.repository.PedidoRepository;
import br.com.aweb.sistema_vendas.repository.ProdutoRepository;

@Service
public class PedidoService {

    private final PedidoRepository pedidoRepository;
    private final ClienteRepository clienteRepository;
    private final ProdutoRepository produtoRepository;

    public PedidoService(
            PedidoRepository pedidoRepository,
            ClienteRepository clienteRepository,
            ProdutoRepository produtoRepository) {

        this.pedidoRepository = pedidoRepository;
        this.clienteRepository = clienteRepository;
        this.produtoRepository = produtoRepository;
    }

    // CREATE
    @Transactional
    public Pedido criarPedido(Cliente cliente) {

        if (cliente == null || cliente.getId() == null) {
            throw new IllegalArgumentException(
                "Cliente é obrigatório."
            );
        }

        Cliente clienteExistente =
                clienteRepository.findById(cliente.getId())
                    .orElseThrow(() ->
                        new IllegalArgumentException(
                            "Cliente não encontrado."
                        )
                    );

        Pedido pedido = new Pedido(clienteExistente);

        pedido.setDataPedido(LocalDateTime.now());
        pedido.setValorTotal(BigDecimal.ZERO);
        pedido.setStatus(StatusPedido.ATIVO);

        return pedidoRepository.save(pedido);
    }

    // READ
    @Transactional(readOnly = true)
    public Optional<Pedido> buscarPorId(Long id) {
        return pedidoRepository.findById(id);
    }

    // READ
    @Transactional(readOnly = true)
    public List<Pedido> listarTodos() {
        return pedidoRepository.findAll();
    }

    // READ POR STATUS
    @Transactional(readOnly = true)
    public List<Pedido> listarPorStatus(
            StatusPedido status) {

        return pedidoRepository.findByStatus(status);
    }

    // ADICIONAR ITEM
    @Transactional
    public void adicionarItem(
            Long pedidoId,
            Long produtoId,
            Integer quantidade) {

        if (quantidade == null || quantidade <= 0) {
            throw new IllegalArgumentException(
                "A quantidade deve ser maior que zero."
            );
        }

        Pedido pedido = pedidoRepository.findById(pedidoId)
                .orElseThrow(() ->
                    new IllegalArgumentException(
                        "Pedido não encontrado."
                    )
                );

        verificarPedidoAtivo(pedido);

        Produto produto = produtoRepository.findById(produtoId)
                .orElseThrow(() ->
                    new IllegalArgumentException(
                        "Produto não encontrado."
                    )
                );

        if (produto.getQuantidadeEmEstoque() < quantidade) {
            throw new IllegalStateException(
                "Estoque insuficiente para o produto: "
                + produto.getNome()
            );
        }

        ItemPedido itemExistente =
                pedido.getItens()
                    .stream()
                    .filter(item ->
                        item.getProduto()
                            .getId()
                            .equals(produtoId))
                    .findFirst()
                    .orElse(null);

        if (itemExistente != null) {

            itemExistente.setQuantidade(
                itemExistente.getQuantidade()
                    + quantidade
            );

        } else {

            ItemPedido item =
                    new ItemPedido(produto, quantidade);

            item.setPedido(pedido);

            pedido.getItens().add(item);
        }

        produto.setQuantidadeEmEstoque(
            produto.getQuantidadeEmEstoque()
                - quantidade
        );

        produtoRepository.save(produto);

        calcularValorTotal(pedido);

        pedidoRepository.save(pedido);
    }

    // ALTERAR QUANTIDADE
    @Transactional
    public void alterarQuantidadeItem(
            Long pedidoId,
            Long itemId,
            Integer novaQuantidade) {

        if (novaQuantidade == null || novaQuantidade <= 0) {
            throw new IllegalArgumentException(
                "A quantidade deve ser maior que zero."
            );
        }

        Pedido pedido = pedidoRepository.findById(pedidoId)
                .orElseThrow(() ->
                    new IllegalArgumentException(
                        "Pedido não encontrado."
                    )
                );

        verificarPedidoAtivo(pedido);

        ItemPedido item =
                localizarItem(pedido, itemId);

        Produto produto = item.getProduto();

        int diferenca =
                novaQuantidade - item.getQuantidade();

        if (diferenca > 0) {

            if (produto.getQuantidadeEmEstoque()
                    < diferenca) {

                throw new IllegalStateException(
                    "Estoque insuficiente para o produto: "
                    + produto.getNome()
                );
            }

            produto.setQuantidadeEmEstoque(
                produto.getQuantidadeEmEstoque()
                    - diferenca
            );

        } else if (diferenca < 0) {

            produto.setQuantidadeEmEstoque(
                produto.getQuantidadeEmEstoque()
                    + Math.abs(diferenca)
            );
        }

        item.setQuantidade(novaQuantidade);

        produtoRepository.save(produto);

        calcularValorTotal(pedido);

        pedidoRepository.save(pedido);
    }

    // REMOVER ITEM
    @Transactional
    public void removerItem(
            Long pedidoId,
            Long itemId) {

        Pedido pedido = pedidoRepository.findById(pedidoId)
                .orElseThrow(() ->
                    new IllegalArgumentException(
                        "Pedido não encontrado."
                    )
                );

        verificarPedidoAtivo(pedido);

        ItemPedido item =
                localizarItem(pedido, itemId);

        Produto produto = item.getProduto();

        // Devolve ao estoque.
        produto.setQuantidadeEmEstoque(
            produto.getQuantidadeEmEstoque()
                + item.getQuantidade()
        );

        produtoRepository.save(produto);

        // orphanRemoval=true remove o item do banco.
        pedido.getItens().remove(item);

        calcularValorTotal(pedido);

        pedidoRepository.save(pedido);
    }

    // CANCELAR PEDIDO
    @Transactional
    public void cancelarPedido(Long pedidoId) {

        Pedido pedido = pedidoRepository.findById(pedidoId)
                .orElseThrow(() ->
                    new IllegalArgumentException(
                        "Pedido não encontrado."
                    )
                );

        if (pedido.getStatus()
                == StatusPedido.CANCELADO) {

            throw new IllegalStateException(
                "Este pedido já está cancelado."
            );
        }

        // Devolve todos os produtos ao estoque.
        for (ItemPedido item : pedido.getItens()) {

            Produto produto = item.getProduto();

            produto.setQuantidadeEmEstoque(
                produto.getQuantidadeEmEstoque()
                    + item.getQuantidade()
            );

            produtoRepository.save(produto);
        }

        pedido.setStatus(StatusPedido.CANCELADO);

        pedidoRepository.save(pedido);
    }

    // CALCULAR TOTAL
    private void calcularValorTotal(Pedido pedido) {

        BigDecimal total = BigDecimal.ZERO;

        for (ItemPedido item : pedido.getItens()) {

            BigDecimal valorItem =
                    item.getPrecoUnitario()
                        .multiply(
                            BigDecimal.valueOf(
                                item.getQuantidade()
                            )
                        );

            total = total.add(valorItem);
        }

        pedido.setValorTotal(total);
    }

    private void verificarPedidoAtivo(
            Pedido pedido) {

        if (pedido.getStatus()
                != StatusPedido.ATIVO) {

            throw new IllegalStateException(
                "Não é possível alterar pedido cancelado."
            );
        }
    }

    private ItemPedido localizarItem(
            Pedido pedido,
            Long itemId) {

        return pedido.getItens()
                .stream()
                .filter(item ->
                    item.getId() != null
                    && item.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() ->
                    new IllegalArgumentException(
                        "Item não encontrado no pedido."
                    )
                );
    }
}