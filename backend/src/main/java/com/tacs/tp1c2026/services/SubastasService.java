package com.tacs.tp1c2026.services;

import com.tacs.tp1c2026.entities.FiguritaColeccion;
import com.tacs.tp1c2026.entities.ItemOfertaSubasta;
import com.tacs.tp1c2026.entities.OfertaSubasta;
import com.tacs.tp1c2026.entities.Subasta;
import com.tacs.tp1c2026.entities.Usuario;
import com.tacs.tp1c2026.entities.dto.input.NuevaSubastaDto;
import com.tacs.tp1c2026.entities.dto.input.NuevaSubastaOfertaDto;
import com.tacs.tp1c2026.entities.dto.output.SubastaDto;
import com.tacs.tp1c2026.entities.enums.EstadoSubasta;
import com.tacs.tp1c2026.exceptions.*;
import com.tacs.tp1c2026.exceptions.FiguritaNoEncontradaException;
import com.tacs.tp1c2026.exceptions.OfertaYaProcesadaException;
import com.tacs.tp1c2026.exceptions.SubastaCerradaException;
import com.tacs.tp1c2026.repositories.OfertasSubastaRepository;
import com.tacs.tp1c2026.repositories.SubastaRepository;
import com.tacs.tp1c2026.repositories.UsuariosRepository;

import java.util.*;

import com.tacs.tp1c2026.services.mappers.SubastaMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SubastasService {

  private final UsuariosRepository usuariosRepository;
  private final SubastaRepository subastaRepository;
  private final OfertasSubastaRepository ofertasSubastaRepository;
  private final SubastaMapper subastaMapper;

  public SubastasService( UsuariosRepository usuariosRepository,SubastaRepository subastaRepository, OfertasSubastaRepository ofertasSubastaRepository, SubastaMapper mapper){
    this.usuariosRepository = usuariosRepository;
    this.subastaRepository = subastaRepository;
    this.ofertasSubastaRepository = ofertasSubastaRepository;
    this.subastaMapper = mapper;
  }


  /**
   * Crea una nueva subasta para una figurita repetida del usuario.
   * Valida los datos mínimos requeridos, verifica que el usuario posea la figurita indicada
   * y que ésta esté habilitada para participar en subastas.
   *
   * @param userId identificador del usuario que crea la subasta
   * @param sub    datos de la nueva subasta (figurita, duración y cantidad mínima)
   * @return identificador de la subasta recién creada
   * @throws UserNotFoundException si el usuario no existe
   * @throws NotFoundException     si el usuario no posee la figurita indicada como repetida
   * @throws ConflictException     si la figurita no está habilitada para participar en subastas
   * @throws BadInputException     si los datos de la subasta no cumplen los requisitos mínimos
   */
  public Integer crearSubasta(Integer userId, NuevaSubastaDto sub) {

    validate(sub);

    Usuario usuario = usuariosRepository.findById(userId).orElseThrow(() -> new UserNotFoundException("No se encontro el usuario"));

    FiguritaColeccion figuritaPublicada;
    try {
      figuritaPublicada = usuario.getRepetidaByNumero(sub.getNumFiguritaPublicada());
    } catch (FiguritaNoEncontradaException e) {
      throw new BadInputException(e.getMessage());
    }

    Subasta subasta = null;
    try {
        subasta = figuritaPublicada.subastar(
                sub.getCantidadMinFiguritas(),
                sub.getDuracionSubastaHs()
        );
    } catch (FiguritasInsuficientesException e) {
        throw new BadInputException("No hay suficientes figuritas para crear la subasta");
    }

      return subastaRepository.save(subasta).getId();
  }


  /**
   * Registra una oferta sobre una subasta activa.
   * Verifica que la subasta exista y esté activa, que el postor no sea el dueño de la subasta,
   * que la subasta no haya vencido y que las figuritas ofrecidas pertenezcan al postor
   * y estén habilitadas para subastas. Actualiza la mejor oferta si corresponde.
   *
   * @param userId       identificador del usuario postor
   * @param subastaId    identificador de la subasta
   * @param nuevaOferta  datos de la oferta, incluyendo la lista de figuritas a ofrecer
   * @throws UserNotFoundException  si el usuario no existe
   * @throws NotFoundException      si la subasta no existe
   * @throws UnauthorizedException  si el postor intenta ofertar en su propia subasta
   * @throws ConflictException      si la subasta no está activa o ya venció
   * @throws BadInputException      si las figuritas son inválidas o insuficientes
   */
  public void ofertarSubasta(Integer userId, Integer subastaId, NuevaSubastaOfertaDto nuevaOferta){


    validate(nuevaOferta);

    Usuario postor = usuariosRepository.findById(userId).orElseThrow(() -> new UserNotFoundException("No se encontro el usuario"));
    Subasta subasta = subastaRepository.findById(subastaId).orElseThrow(() -> new NotFoundException("No se encontro la subasta"));

    Map<FiguritaColeccion, Integer> repetidasPostor = new HashMap<>();

    nuevaOferta.getItemsOfertados().forEach(item -> {
      FiguritaColeccion repetida;
      try {
        repetida = postor.getRepetidaByNumero(item.getFiguritaId());
      } catch (FiguritaNoEncontradaException e) {
        throw new BadInputException(e.getMessage());
      }
      repetidasPostor.put(repetida, item.getCantidad());
    });

    repetidasPostor.forEach((key, value) -> {
        if (!key.getUsuario().equals(postor)) {
            throw new BadInputException("El usuario no posee la figurita " + key.getFigurita().getNumero());
        }
        if (key.noTieneSuficientes(value)) {
            throw new BadInputException("El usuario no posee suficientes duplicadas de la figurita " + key.getFigurita().getNumero());
        }
    });

    List<ItemOfertaSubasta> itemsOfrecidos = repetidasPostor.entrySet().stream()
        .map(entry -> {
            try {
                entry.getKey().reducirDisponible(entry.getValue());
            } catch (FiguritasInsuficientesException e) {
                throw new BadInputException("No hay suficientes figuritas para ofrecer en la oferta");
            }
            return new ItemOfertaSubasta(
                  entry.getKey().getFigurita(),
                  entry.getValue()
          );
                }).toList();


    OfertaSubasta ofertaSubasta = new OfertaSubasta(
      postor,
      subasta,
      itemsOfrecidos
    );

    subasta.agregarOferta(ofertaSubasta);

  }


  /**
   * Acepta una oferta de subasta pendiente y adjudica la subasta al postor.
   * Rechaza automáticamente las demás ofertas pendientes de la misma subasta.
   *
   * @param userId    identificador del usuario dueño de la subasta
   * @param subastaId identificador de la subasta
   * @param ofertaId  identificador de la oferta a aceptar
   * @throws UserNotFoundException  si el usuario no existe
   * @throws NotFoundException      si la subasta o la oferta no existen
   * @throws BadInputException      si la oferta no corresponde a la subasta indicada
   * @throws UnauthorizedException  si el usuario no es el dueño de la subasta
   * @throws ConflictException      si la subasta no permite aceptar ofertas o la oferta ya fue procesada
   */
  public void aceptarOfertaSubasta(Integer userId, Integer subastaId, Integer ofertaId) {

    Usuario usuario = usuariosRepository.findById(userId).orElseThrow(() -> new UserNotFoundException("No se encontro el usuario"));
    Subasta subasta = subastaRepository.findById(subastaId).orElseThrow(() -> new NotFoundException("No se encontro la subasta"));
    OfertaSubasta oferta = ofertasSubastaRepository.findById(ofertaId).orElseThrow(() -> new NotFoundException("No se encontro la oferta"));

    if (!subasta.getUsuarioPublicante().equals(usuario)) {
      throw new UnauthorizedException("El usuario no es el dueño de la subasta");
    }

    try {
      subasta.aceptarOferta(oferta);
    } catch (SubastaCerradaException | OfertaYaProcesadaException e) {
      throw new ConflictException(e.getMessage());
    } catch (IllegalArgumentException e) {
      throw new BadInputException(e.getMessage());
    }

    ofertasSubastaRepository.save(oferta);
    subastaRepository.save(subasta);
  }


  /**
   * Rechaza una oferta de subasta pendiente.
   * Sólo el dueño de la subasta puede rechazar ofertas.
   *
   * @param userId    identificador del usuario dueño de la subasta
   * @param subastaId identificador de la subasta
   * @param ofertaId  identificador de la oferta a rechazar
   * @throws UserNotFoundException  si el usuario no existe
   * @throws NotFoundException      si la subasta o la oferta no existen
   * @throws BadInputException      si la oferta no corresponde a la subasta indicada
   * @throws UnauthorizedException  si el usuario no es el dueño de la subasta
   * @throws ConflictException      si la oferta ya fue aceptada o rechazada previamente
   */
  public void rechazarOfertaSubasta(Integer userId, Integer subastaId, Integer ofertaId) {
    Usuario usuario = usuariosRepository.findById(userId).orElseThrow(() -> new UserNotFoundException("No se encontro el usuario"));
    Subasta subasta = subastaRepository.findById(subastaId).orElseThrow(() -> new NotFoundException("No se encontro la subasta"));
    OfertaSubasta oferta = ofertasSubastaRepository.findById(ofertaId).orElseThrow(() -> new NotFoundException("No se encontro la oferta"));

    if (!subasta.getUsuarioPublicante().equals(usuario)) {
      throw new UnauthorizedException("El usuario no es el dueño de la subasta");
    }

    try {
      subasta.rechazarOferta(oferta);
    } catch (OfertaYaProcesadaException e) {
      throw new ConflictException(e.getMessage());
    } catch (IllegalArgumentException e) {
      throw new BadInputException(e.getMessage());
    }

    ofertasSubastaRepository.save(oferta);
  }


  public List<SubastaDto> obtenerSubastasActivasGlobales() {    return subastaRepository.findByEstado(EstadoSubasta.ACTIVA)
            .stream()
            .map(subastaMapper::mapSubasta)
            .toList();
  }



  /**
   * Valida que los datos mínimos requeridos para crear una subasta sean correctos.
   *
   * @param dto datos del DTO de nueva subasta
   * @throws BadInputException si falta la figurita, la duración es insuficiente
   *                           o la cantidad mínima de figuritas es inválida
   */
  private void validate(NuevaSubastaDto dto) {

    if (dto.getNumFiguritaPublicada() == null) {
      throw new BadInputException("Debe indicar la figurita a subastar");
    }

    if (dto.getDuracionSubastaHs() == null || dto.getDuracionSubastaHs() <= 1) {
      throw new BadInputException("La duracion de la subasta debe ser mayor a 1 hora");
    }

    if (dto.getCantidadMinFiguritas() == null || dto.getCantidadMinFiguritas() < 1) {
      throw new BadInputException("La cantidad minima de figuritas debe ser mayor que 0");
    }
  }

  /**
   * Valida los datos mínimos requeridos para ofertar en una subasta.
   *
   * @param dto DTO de la nueva oferta
   * @throws BadInputException si la oferta es nula o no incluye al menos 1 item
   */
  private void validate(NuevaSubastaOfertaDto dto) {
    if (dto == null || dto.getItemsOfertados() == null || dto.getItemsOfertados().isEmpty()) {
      throw new BadInputException("Se necesita ofrecer como minimo 1 item");
    }
   }



  private SubastaDto mapSubasta(Subasta subasta) {
    return subastaMapper.mapSubasta(subasta);
  }


  /**
   * ...existing code...
   */
  @Transactional
  public void agregarUsuarioInteresado(Integer subastaId, Integer userId) {
    Usuario usuario = usuariosRepository.findById(userId)
        .orElseThrow(() -> new NotFoundException("Usuario no encontrado con id: " + userId));

    Subasta subasta = subastaRepository.findById(subastaId)
        .orElseThrow(() -> new NotFoundException("Subasta no encontrada con id: " + subastaId));

    subasta.agregarInteresado(usuario);
    subastaRepository.save(subasta);
  }

}
