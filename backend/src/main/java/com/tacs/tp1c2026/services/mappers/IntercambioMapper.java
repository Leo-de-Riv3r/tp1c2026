package com.tacs.tp1c2026.services.mappers;

import com.tacs.tp1c2026.entities.ExchangeProposal;
import com.tacs.tp1c2026.entities.ExchangePublication;
import com.tacs.tp1c2026.entities.dto.output.ExchangePublicationDto;
import com.tacs.tp1c2026.entities.dto.output.ExchangeProposalDto;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Mapper para convertir entidades de intercambio a sus representaciones DTO.
 */
@Component
public class IntercambioMapper {

  /**
   * Convierte una {@link ExchangePublication} a su representación DTO.
   *
   * @param publication entidad de publicación de intercambio
   * @return {@link ExchangePublicationDto} con los datos básicos de la publicación
   */
  public ExchangePublicationDto mapPublication(ExchangePublication publication) {
    return new ExchangePublicationDto(
        publication.getId(),
        publication.getQuantity(),
        publication.getCard().getNumber(),
        publication.getState(),
        publication.getName(),
        publication.getDescription(),
        publication.getCountry(),
        publication.getTeam(),
        publication.getCategory()
    );
  }

  public List<ExchangePublicationDto> mapPublications(List<ExchangePublication> publication) {
    return publication.stream().map(this::mapPublication).toList();
  }
  /**
   * Convierte una {@link ExchangeProposal} a su representación DTO.
   *
   * @param propuesta entidad de propuesta de intercambio
   * @return {@link ExchangeProposalDto} con el estado, figuritas ofrecidas y referencias asociadas
   */
  public ExchangeProposalDto mapPropuesta(ExchangeProposal propuesta) {

    return new ExchangeProposalDto(
        propuesta.getId(),
        propuesta.getPublication().getId(),
        propuesta.getCards(),
        propuesta.getExchangeUser().getId(),
        propuesta.getState(),
        propuesta.getCreationDate()
        );
  }


  /**{
   * Convierte una lista de {@link ExchangeProposal} a una lista de {@link ExchangeProposalDto}.
   * @param proposals lista de propuestas de intercambio
   * @return lista de DTOs de propuestas de intercambio
   */
  public List<ExchangeProposalDto> mapProposals(List<ExchangeProposal> proposals) {
    return proposals.stream().map(this::mapPropuesta).toList();
  }
}


