package agendo.app.server.modules.rating.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import agendo.app.server.modules.rating.models.RatingEntity;
import agendo.app.server.modules.rating.repository.RatingRepository;
import agendo.app.server.modules.user.repository.ProfessionalProfileRepository;
import agendo.app.server.modules.user.models.UserEntity;
import agendo.app.server.modules.user.models.UserRole;

/**
 * Testes unitários do RatingService — cobre as regras de quem pode avaliar
 * e quem pode ser avaliado.
 *
 * Nota: o construtor exige um ProfessionalProfileRepository porque a classe
 * o declara como dependência (atualmente sem uso real — ver o relatório de
 * qualidade de código). Ele é mockado aqui apenas para satisfazer o
 * construtor; nenhum teste interage com ele.
 */
@ExtendWith(MockitoExtension.class)
class RatingServiceTest {

    @Mock
    private RatingRepository ratingRepository;

    @Mock
    private ProfessionalProfileRepository professionalProfileRepository;

    private RatingService ratingService;

    private UserEntity client;
    private UserEntity professional;

    @BeforeEach
    void setUp() {
        ratingService = new RatingService(ratingRepository, professionalProfileRepository);

        client = UserEntity.builder().id(1L).name("Cliente").role(UserRole.CLIENT).build();
        professional = UserEntity.builder().id(2L).name("Profissional").role(UserRole.PROFESSIONAL).build();
    }

    @Test
    void create_clienteAvaliandoProfissional_salvaComSucesso() {
        RatingEntity rating = RatingEntity.builder().score(5).client(client).professional(professional).build();
        when(ratingRepository.save(rating)).thenReturn(rating);

        RatingEntity result = ratingService.create(rating);

        assertThat(result).isEqualTo(rating);
        verify(ratingRepository).save(rating);
    }

    @Test
    void create_avaliadorNaoClienteLancaForbidden() {
        // um PROFESSIONAL tentando avaliar -> não é permitido
        RatingEntity rating = RatingEntity.builder().score(5).client(professional).professional(professional).build();

        assertThatThrownBy(() -> ratingService.create(rating))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("403");

        verify(ratingRepository, never()).save(any());
    }

    @Test
    void create_avaliadoNaoProfissionalLancaBadRequest() {
        // tentando avaliar um CLIENT como se fosse profissional -> inválido
        UserEntity outroCliente = UserEntity.builder().id(3L).role(UserRole.CLIENT).build();
        RatingEntity rating = RatingEntity.builder().score(5).client(client).professional(outroCliente).build();

        assertThatThrownBy(() -> ratingService.create(rating))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("400");

        verify(ratingRepository, never()).save(any());
    }

    @Test
    void getAverageScore_retornaZeroQuandoNaoHaAvaliacoes() {
        when(ratingRepository.getAverageScoreByProfessionalId(2L)).thenReturn(null);

        Double average = ratingService.getAverageScore(2L);

        assertThat(average).isEqualTo(0.0);
    }

    @Test
    void getAverageScore_retornaMediaCalculadaPeloRepositorio() {
        when(ratingRepository.getAverageScoreByProfessionalId(2L)).thenReturn(4.5);

        Double average = ratingService.getAverageScore(2L);

        assertThat(average).isEqualTo(4.5);
    }
}
