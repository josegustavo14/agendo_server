package agendo.app.server.modules.rating.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import agendo.app.server.modules.rating.models.RatingEntity;
import agendo.app.server.modules.rating.repository.RatingRepository;
import agendo.app.server.modules.user.models.UserEntity;
import agendo.app.server.modules.user.models.UserRole;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RatingService {

    private final RatingRepository ratingRepository;

    @Transactional
    public RatingEntity create(RatingEntity rating) {
        if (rating.getClient().getRole() != UserRole.CLIENT) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Apenas clientes podem avaliar profissionais");
        }

        if (rating.getProfessional().getRole() != UserRole.PROFESSIONAL) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O usuário avaliado deve ser um profissional");
        }

        return ratingRepository.save(rating);
    }

    public List<RatingEntity> findByProfessionalId(Long professionalId) {
        return ratingRepository.findByProfessionalId(professionalId);
    }

    public List<RatingEntity> findByClient(UserEntity client) {
        return ratingRepository.findByClient(client);
    }

    public Double getAverageScore(Long professionalId) {
        Double avg = ratingRepository.getAverageScoreByProfessionalId(professionalId);
        return avg != null ? avg : 0.0;
    }

    public Long countByProfessionalId(Long professionalId) {
        return ratingRepository.countByProfessionalId(professionalId);
    }
}