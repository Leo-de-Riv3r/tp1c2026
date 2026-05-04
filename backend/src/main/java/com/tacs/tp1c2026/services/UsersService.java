package com.tacs.tp1c2026.services;

import com.tacs.tp1c2026.entities.user.User;
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

//	public User createUsuario(String nombre) {
//		User usuario = new User();
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

    public User getUserById(String userId){
      return usersRepository.findById(userId).orElseThrow(() -> new UserNotFoundException("No se encontro el usuario"));
    }

  /**
   *
   * @param user servira tanto para guardar uno nuevo como para actualizar
   */
  public void saveUser(User user) {
      usersRepository.save(user);
    }

  public void saveUsers(List<User> usersToModify) {
    usersRepository.saveAll(usersToModify);
  }

  public List<User> getAll() {
    return usersRepository.findAll();
  }
}

