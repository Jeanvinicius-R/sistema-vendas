package br.com.aweb.sistema_vendas.model;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import jakarta.validation.Validation;
import jakarta.validation.Validator;

class ClienteCpfTest {

    private final Validator validator =
        Validation.buildDefaultValidatorFactory().getValidator();

    private boolean cpfTemErro(String cpf) {
        var cliente = new Cliente();
        cliente.setCpf(cpf);
        return !validator.validateProperty(cliente, "cpf").isEmpty();
    }

    @Test
    void aceitaCpfValidoNoFormatoComMascara() {
        assertFalse(cpfTemErro("529.982.247-25"));
    }

    @Test
    void rejeitaCpfSemMascara() {
        assertTrue(cpfTemErro("52998224725"));
    }

    @Test
    void rejeitaQuantidadeDeDigitosDiferenteDe11() {
        assertTrue(cpfTemErro("529.982.247-2"));
        assertTrue(cpfTemErro("529.982.247-255"));
    }

    @Test
    void rejeitaCaracteresNaoNumericos() {
        assertTrue(cpfTemErro("abc.982.247-25"));
    }

    @Test
    void rejeitaCpfComDigitoVerificadorInvalido() {
        assertTrue(cpfTemErro("123.456.789-00"));
    }
}
