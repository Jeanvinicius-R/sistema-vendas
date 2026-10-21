package br.com.aweb.sistema_vendas.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import br.com.aweb.sistema_vendas.model.Usuario;
import br.com.aweb.sistema_vendas.model.UsuarioRole;
import br.com.aweb.sistema_vendas.repository.UsuarioRepository;

// Cria um ADMIN e um PADRAO de teste, caso ainda não existam
@Component
public class UsuariosIniciais implements ApplicationRunner {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.seed.admin-email}")
    private String adminEmail;

    @Value("${app.seed.admin-senha}")
    private String adminSenha;

    @Value("${app.seed.padrao-email}")
    private String padraoEmail;

    @Value("${app.seed.padrao-senha}")
    private String padraoSenha;

    public UsuariosIniciais(
            UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder) {

        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        criarSeNaoExistir("Administrador", adminEmail, adminSenha, UsuarioRole.ADMIN);
        criarSeNaoExistir("Usuário Padrão", padraoEmail, padraoSenha, UsuarioRole.PADRAO);
    }

    private void criarSeNaoExistir(String nome, String email, String senha, UsuarioRole role) {
        if (usuarioRepository.findByEmailIgnoreCase(email).isEmpty()) {
            usuarioRepository.save(new Usuario(nome, email, passwordEncoder.encode(senha), role));
        }
    }
}
