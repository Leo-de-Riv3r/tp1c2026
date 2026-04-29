package com.tacs.tp1c2026.services;

import com.tacs.tp1c2026.entities.*;
import com.tacs.tp1c2026.entities.dto.output.*;
import com.tacs.tp1c2026.entities.enums.AuctionState;
import com.tacs.tp1c2026.exceptions.BadInputException;
import com.tacs.tp1c2026.exceptions.UserNotFoundException;
import com.tacs.tp1c2026.repositories.AuctionRepository;
import com.tacs.tp1c2026.repositories.ExchangeProposalsRepository;
import com.tacs.tp1c2026.repositories.ExchangePublicationsRepository;
import com.tacs.tp1c2026.repositories.UsersRepository;
import com.tacs.tp1c2026.services.mappers.IntercambioMapper;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UsersService {
  @Autowired
	  private UsersRepository usersRepository;
  @Autowired
	  private ExchangePublicationsRepository exchangePublicationsRepository;
  @Autowired
	  private ExchangeProposalsRepository exchangeProposalsRepository;
  @Autowired
  private AuctionRepository auctionRepository;
//  @Autowired
//	  private SubastaMapper subastaMapper;
  @Autowired
	  private IntercambioMapper intercambioMapper;

//	public Usuario createUsuario(String nombre) {
//		Usuario usuario = new Usuario();
//		usuario.set(nombre);
//		usuario.setFechaAlta(java.time.LocalDateTime.now());
//		return usersRepository.save(usuario);
//	}

//	public List<UsuarioDto> listarUsuarios() {
//		return usersRepository.findAll().stream().map(u -> {
//			UsuarioDto dto = new UsuarioDto();
//			dto.setId(u.getId());
//			dto.setNombre(u.getNombre());
//			dto.setFechaAlta(u.getFechaAlta());
//			return dto;
//		}).toList();
//	}
//


  /**
   * Valida que el usuario con el identificador indicado exista en el repositorio.
   *
   * @param userId identificador del usuario a validar
   * @throws UserNotFoundException si el usuario no existe
   */
	  public void validateUserExists(String userId) {
		  usersRepository.findById(userId).orElseThrow(() -> new UserNotFoundException("No se encontro el usuario"));
	  }

    public Usuario getUserById(String userId){
      return usersRepository.findById(userId).orElseThrow(() -> new UserNotFoundException("No se encontro el usuario"));
    }

  /**
   *
   * @param usuario servira tanto para guardar uno nuevo como para actualizar
   */
  public void saveUser(Usuario usuario) {
      usersRepository.save(usuario);
    }

  public void saveUsers(List<Usuario> usersToModify) {
    usersRepository.saveAll(usersToModify);
  }
}

