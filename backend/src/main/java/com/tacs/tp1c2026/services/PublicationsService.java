package com.tacs.tp1c2026.services;

import com.tacs.tp1c2026.entities.Card;
import com.tacs.tp1c2026.entities.CardCollection;
import com.tacs.tp1c2026.entities.ExchangeProposal;
import com.tacs.tp1c2026.entities.ExchangePublication;
import com.tacs.tp1c2026.entities.Feedback;
import com.tacs.tp1c2026.entities.Usuario;
import com.tacs.tp1c2026.entities.dto.input.NewExchangeProposalDto;
import com.tacs.tp1c2026.entities.dto.input.NewFeedbackDto;
import com.tacs.tp1c2026.entities.dto.input.NewExchangePublicationDto;
import com.tacs.tp1c2026.entities.dto.input.ReviewProposalDto;
import com.tacs.tp1c2026.entities.dto.input.SearchPublicationsFilters;
import com.tacs.tp1c2026.entities.dto.output.ExchangePublicationDto;
import com.tacs.tp1c2026.entities.dto.output.PaginacionDto;
import com.tacs.tp1c2026.entities.dto.output.PaginationDtoOutput;
import com.tacs.tp1c2026.entities.dto.output.ExchangeProposalDto;

import com.tacs.tp1c2026.entities.dto.output.PublicacionDto;
import com.tacs.tp1c2026.entities.enums.ProposalState;
import com.tacs.tp1c2026.entities.enums.PublicationState;
import com.tacs.tp1c2026.entities.enums.ReviewAction;
import com.tacs.tp1c2026.entities.enums.ParticipationType;
import com.tacs.tp1c2026.exceptions.BadInputException;
import com.tacs.tp1c2026.exceptions.ConflictException;

import com.tacs.tp1c2026.exceptions.NotFoundException;
import com.tacs.tp1c2026.exceptions.UnauthorizedException;
import com.tacs.tp1c2026.exceptions.UserNotFoundException;
import com.tacs.tp1c2026.repositories.ExchangeProposalsRepository;
import com.tacs.tp1c2026.repositories.ExchangePublicationsRepository;
import com.tacs.tp1c2026.repositories.UsersRepository;
import com.tacs.tp1c2026.services.mappers.IntercambioMapper;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class PublicationsService {
  @Autowired
  private ExchangeProposalsRepository exchangeProposalsRepository;
  @Autowired
  private UsersRepository usersRepository;
  @Autowired
  private ExchangePublicationsRepository exchangePublicationsRepository;
  @Autowired
  private IntercambioMapper intercambioMapper;
  @Autowired
  private UsersService usersService;
  @Autowired
  private AlertService alertService;

  /**
   * Crea una publicación de intercambio para una card repetida del usuario.
   * Verifica que el usuario exista y que la card indicada esté en su colección de repetidas.
   *
   * @param userId     identificador del usuario que realiza la publicación
   * @throws UserNotFoundException si el usuario no existe
   * @throws NotFoundException     si la card no se encuentra entre las repetidas del usuario
   */
  @Transactional
  public String publishCardExchange(NewExchangePublicationDto dto, String userId) {
    Usuario cardOwner = usersService.getUserById(userId);
    //verificar que el cardOwner tiene la card en su coleccion
    Optional<CardCollection> itemUser = cardOwner.getCollection().stream().filter(
        f -> f.getCard().getId().equals(dto.getCardId())).findFirst();
    if (itemUser.isEmpty()) {
      throw new ConflictException("No tiene la card en su coleccion");
    }

    CardCollection item = itemUser.get();
    if (!item.canPublishExchange(dto.getQuantity())) {
      throw new ConflictException("No tiene suficientes figuritas para publicar");
    }

    ExchangePublication publication = ExchangePublication.builder()
        .publisherUser(cardOwner)
        .card(item.getCard())
        .quantity(dto.getQuantity())
        .number(item.getCard().getNumber())
        .name(item.getCard().getName())
        .description(item.getCard().getDescription())
        .country(item.getCard().getCountry())
        .team(item.getCard().getTeam())
        .category(item.getCard().getCategory())
        .build();

    ExchangePublication createdPublication = exchangePublicationsRepository.save(publication);
    item.addCompromisedQuantity(dto.getQuantity(), ParticipationType.INTERCAMBIO);
    usersService.saveUser(cardOwner);
    return createdPublication.getId();
  }

  /**
   * Registra una propuesta de intercambio sobre una publicación existente.
   * Valida que haya cupos disponibles, que las figuritas ofrecidas pertenezcan al proponente
   * y que estén habilitadas para participar en intercambios.
   *
   * @return la {@link ExchangeProposal} persistida
   * @throws UserNotFoundException si el usuario no existe
   * @throws NotFoundException     si la publicación no existe
   * @throws ConflictException     si ya no hay cupos disponibles en la publicación
   * @throws BadInputException     si alguna de las figuritas no se encuentra o no está habilitada para intercambio
   */
  @Transactional
  public ExchangeProposal offerProposalExchange(String userId, NewExchangeProposalDto dto) {
    Usuario offererUser = usersService.getUserById(userId);

    ExchangePublication publication = getById(dto.getPublicationId());

    if (publication.getPublisherUser().getId().equals(userId)) {
      throw new ConflictException("No podés proponer un intercambio en tu propia publicación");
    }

    // Validar cupos disponibles
    publication.validarCuposDisponibles();

    // Obtener y validar figuritas para oferta
    List<Card> cardsToOffer = offererUser.obtenerFiguritasParaOferta(dto.getCardsIds());

    ExchangeProposal proposal = new ExchangeProposal(publication, cardsToOffer, offererUser, publication.getPublisherUser());

    usersService.saveUser(offererUser);
    proposal = exchangeProposalsRepository.save(proposal);

    publication.addProposal(proposal);
    exchangePublicationsRepository.save(publication);

    alertService.notificarPropuestaRecibida(proposal);
    return proposal;
  }

  /**
   * Retorna todas las propuestas de intercambio recibidas sobre las publicaciones propias del usuario.
   *
   * @param userId identificador del usuario publicante
   * @throws UserNotFoundException si el usuario no existe
   */
  public PaginationDtoOutput<ExchangeProposalDto> getReceivedProposals(String userId, ProposalState state, Integer page, Integer per_page) {
    Usuario publisherUser = usersService.getUserById(userId);

    Pageable pageable = this.buildPageable(
        page,
        per_page,
        15,
        Sort.by("creationDate").descending()
    );

    Page<ExchangeProposal> pageResult = exchangeProposalsRepository
        .findByReceiverIdAndState(userId, state, pageable);

    List<ExchangeProposalDto> dtos =  intercambioMapper.mapProposals(pageResult.getContent());

    return new PaginationDtoOutput<>(dtos, pageResult.getNumber() + 1, pageResult.getTotalPages());  }


  public PaginationDtoOutput<ExchangeProposalDto> getOfferedProposals(String userId, ProposalState state, Integer page, Integer per_page) {
    Usuario userToSearch = usersService.getUserById(userId);

    Pageable pageable = this.buildPageable(
        page,
        per_page,
        15,
        Sort.by("creationDate").descending()
    );

    Page<ExchangeProposal> pageResult = exchangeProposalsRepository.findByExchangeUserIdAndState(userId, state, pageable);
    List<ExchangeProposalDto> dtos = intercambioMapper.mapProposals(pageResult.getContent());
    return new PaginationDtoOutput<>(dtos, pageResult.getNumber() + 1, pageResult.getTotalPages());
  }

  /**
   * Acepta una propuesta de intercambio pendiente y ejecuta la transferencia de figuritas.
   * Al aceptar: reduce el stock de la card publicada, agrega las figuritas ofrecidas
   * a la colección del publicante, elimina la card de sus faltantes si corresponde,
   * cierra la publicación cuando el stock llega a cero y rechaza automáticamente el resto
   * de propuestas pendientes.
   *
   * @param userId        identificador del usuario dueño de la publicación
   * @throws UserNotFoundException   si el usuario no existe
   * @throws NotFoundException       si la propuesta o la publicación no existen
   * @throws BadInputException       si la propuesta no corresponde a la publicación indicada
   * @throws UnauthorizedException   si el usuario no es el dueño de la publicación
   * @throws ConflictException       si la propuesta ya fue aceptada o rechazada previamente
   */
  @Transactional
  public void reviewProposal(ReviewProposalDto dto, String userId) {
    Usuario owner = usersService.getUserById(userId);
    ExchangePublication exchangePublication = getById(dto.getPublicationId());
    exchangePublication.validateOwner(owner);

    if(exchangePublication.getState() != PublicationState.ACTIVA) {
      throw new ConflictException("La publicacion no esta activa");
    }

    ExchangeProposal proposalToReview =  exchangePublication.getReceivedProposals().stream().
        filter(exchangeProposal -> exchangeProposal.getId().equals(dto.getProposalId())).
        findFirst().orElseThrow(() -> new NotFoundException("No se encontro la propuesta"));
    if (proposalToReview.getState() != ProposalState.PENDIENTE) {
      throw new ConflictException("La propuesta ya fue revisada");
    }
    Usuario proposer = proposalToReview.getExchangeUser();

    if (dto.getAction() == ReviewAction.REJECT){
      proposalToReview.reject();
      List<String> figuritasId = proposalToReview.getCards().stream().map(Card::getId).toList();
      proposer.restoreFiguritasFromProposal(figuritasId, ParticipationType.INTERCAMBIO);
      exchangeProposalsRepository.save(proposalToReview);
      usersService.saveUser(proposer);
    } else {
      proposalToReview.accept();
      exchangePublication.aceptarPropuesta(proposalToReview, owner);
      exchangeProposalsRepository.save(proposalToReview);

      owner.removeFromCollectionForExchangeAndReceive(List.of(exchangePublication.getCard()), proposalToReview.getCards());
      proposer.removeFromCollectionForExchangeAndReceive(proposalToReview.getCards(), List.of(exchangePublication.getCard()));

      Map<String, Usuario> usuariosAActualizar = new HashMap<>();
      usuariosAActualizar.put(owner.getId(), owner);
      Usuario put = usuariosAActualizar.put(proposer.getId(), proposer);

      if (exchangePublication.getState() == PublicationState.FINALIZADA) {
        List<ExchangeProposal> proposalsToReject = exchangePublication.getReceivedProposals().stream()
            .filter(p -> !p.getId().equals(proposalToReview.getId())).toList();

        proposalsToReject.forEach(proposal -> {
          proposal.reject();
          Usuario user = proposal.getExchangeUser();
          usuariosAActualizar.putIfAbsent(user.getId(), user);
          Usuario userUnico = usuariosAActualizar.get(user.getId());
          userUnico.restoreFiguritasFromProposal(
              proposal.getCards().stream().map(Card::getId).toList(),
              ParticipationType.INTERCAMBIO
          );
        });

        exchangeProposalsRepository.saveAll(proposalsToReject);
      }

      usersService.saveUsers(usuariosAActualizar.values().stream().toList());
      exchangePublicationsRepository.save(exchangePublication);
    }
  }

  public void publishFeedback(String userId, NewFeedbackDto dto) {
    Usuario publisherUser = usersService.getUserById(userId);
    ExchangePublication publication = getById(dto.getPublicationId());

    List<ExchangeProposal> acceptedProposals = publication.getAcceptedProposals().stream().filter(propuesta -> propuesta.getExchangeUser().getId().equals(userId)).toList();

    if (publication.getPublisherUser().getId() != userId) {
      if(acceptedProposals.isEmpty()) {
        throw new ConflictException("No eres dueño de la publicacion o tu propuesta no fue aceptada");
      }
    }

    List<Feedback> feedbacksPublication = publication.getFeedbacks();
    Optional<Feedback> existingFeedback = feedbacksPublication.stream().filter(
        feedback -> feedback.getQualifier().getId().equals(userId)
    ).findFirst();
    if (existingFeedback.isPresent()) {
      throw new ConflictException("Ya dejaste un comentario sobre la publicacion");
    }
    Feedback feedback = Feedback.builder()
        .qualification(dto.getRating())
        .qualifier(publisherUser)
        .comment(dto.getCommentary())
        .build();
    publication.addFeedback(feedback);
    exchangePublicationsRepository.save(publication);
  }

  private Pageable buildPageable(Integer page, Integer perPage, int maxPerPage, Sort sort) {
    int validPage = (page == null || page < 1) ? 0 : page - 1;
    int validSize = (perPage == null || perPage < 1) ? 15 : perPage;
    // 3. Límite de seguridad (Si piden 1000, le damos el máximo permitido)
    if (validSize > maxPerPage) {
      validSize = maxPerPage;
    }

    return sort != null ? PageRequest.of(validPage, validSize, sort) : PageRequest.of(validPage, validSize);
  }
  /**
   * Busca publicaciones de intercambio activas aplicando filtros opcionales y devuelve el resultado paginado.
   *
   * @param page          número de página solicitada (1-based)
   * @param per_page      cantidad de resultados por página
   * @return {@link PaginacionDto} con la lista de {@link PublicacionDto} correspondiente a la página solicitada
   */
  public PaginationDtoOutput<ExchangePublicationDto> searchPublications(
      SearchPublicationsFilters filters, Integer page, Integer per_page
  ) {
    int pageRequested = (page == null || page < 1) ? 1 : page;
    int pageSize = (per_page == null || per_page < 1) ? 10 : per_page;
    Pageable pageable = PageRequest.of(pageRequested - 1, pageSize);

    Page<ExchangePublication> pageResult = exchangePublicationsRepository.searchWithFilters(filters, pageable);

    List<ExchangePublicationDto> dtos = intercambioMapper.mapPublications(pageResult.getContent());
    return new PaginationDtoOutput<>(dtos, pageResult.getNumber() + 1, pageResult.getTotalPages());
  }

  public ExchangePublication getById(String publicacionId) {
    return exchangePublicationsRepository.findById(publicacionId)
        .orElseThrow(() -> new NotFoundException("No se encontro la publicacion"));
  }

  public PaginationDtoOutput<ExchangePublicationDto> getPublicationsCreatedByUser(String userId, Integer page, Integer perPage) {
    Usuario userToSearch = usersService.getUserById(userId);
    Pageable pageable = this.buildPageable(
        page,
        perPage,
        15,
        Sort.by("creationDate").descending()
    );

    Page<ExchangePublication> pageResult = exchangePublicationsRepository.findByPublisherUserId(userId, pageable);
    List<ExchangePublicationDto> dtos = intercambioMapper.mapPublications(pageResult.getContent());
    return new PaginationDtoOutput<>(dtos, pageResult.getNumber() + 1, pageResult.getTotalPages());
  }

  public PaginationDtoOutput<ExchangeProposalDto> getReceivedProposalsByPublication(String userId, String publicationId, Integer page, Integer perPage) {
    Usuario userToSearch = usersService.getUserById(userId);
    Pageable pageable = this.buildPageable(
        page,
        perPage,
        10,
        Sort.by("creationDate").descending()
    );

    Page<ExchangeProposal> pageResult = exchangeProposalsRepository.findByPublicationId(publicationId, pageable);
    List<ExchangeProposalDto> dtos = intercambioMapper.mapProposals(pageResult.getContent());
    return new PaginationDtoOutput<>(dtos, pageResult.getNumber() + 1, pageResult.getTotalPages());
  }
}
