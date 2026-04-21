package com.tacs.tp1c2026.services;

import com.tacs.tp1c2026.entities.Usuario;
import com.tacs.tp1c2026.entities.dto.input.LoginDTO;
import com.tacs.tp1c2026.entities.dto.input.RegisterDTO;
import com.tacs.tp1c2026.entities.dto.output.AuthResponseDto;
import com.tacs.tp1c2026.exceptions.BadInputException;
import com.tacs.tp1c2026.exceptions.ConflictException;
import com.tacs.tp1c2026.exceptions.UnauthorizedException;
import com.tacs.tp1c2026.repositories.UsuariosRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Service
public class AuthService {

  private final UsuariosRepository usuariosRepository;
  private final SecretKey jwtSecretKey;
  private final long jwtExpirationMs;
  private final PasswordEncoder passwordEncoder;

  public AuthService(UsuariosRepository usuariosRepository, @Value("${jwt.secret}") String jwtSecret, @Value("${jwt.expiration}") long jwtExpirationMs) {
	this.usuariosRepository = usuariosRepository;
	this.jwtSecretKey = Keys.hmacShaKeyFor(jwtSecret.getBytes());
	this.jwtExpirationMs = jwtExpirationMs;
	this.passwordEncoder = new BCryptPasswordEncoder();
  }


  public AuthResponseDto register(RegisterDTO registerDTO) {
	validarRegistro(registerDTO);
	String email = registerDTO.getEmail().trim().toLowerCase();

	if (usuariosRepository.existsByEmail(email)) {
	  throw new ConflictException("El email ya se encuentra registrado");
	}

	Usuario nuevoUsuario = new Usuario();
	nuevoUsuario.setNombre(registerDTO.getNombre().trim());
	nuevoUsuario.setEmail(email);
	nuevoUsuario.setPasswordHash(passwordEncoder.encode(registerDTO.getPassword()));

	Usuario usuarioCreado = usuariosRepository.save(nuevoUsuario);
	return buildResponse(usuarioCreado, "Usuario creado con exito");
  }

  public AuthResponseDto login(LoginDTO loginDTO) {
	if (loginDTO == null || isBlank(loginDTO.getEmail()) || isBlank(loginDTO.getPassword())) {
	  throw new BadInputException("Email y password son obligatorios");
	}

	String email = loginDTO.getEmail().trim().toLowerCase();

	Usuario usuario = usuariosRepository.findByEmail(email)
		.orElseThrow(() -> new UnauthorizedException("Credenciales invalidas"));


	if (usuario.getPasswordHash() == null || !passwordEncoder.matches(loginDTO.getPassword(), usuario.getPasswordHash())) {
	  throw new UnauthorizedException("Credenciales invalidas");
	}

	return buildResponse(usuario, "Login exitoso");
  }

  private void validarRegistro(RegisterDTO registerDTO) {
	if (registerDTO == null) {
	  throw new BadInputException("Request de registro invalido");
	}
	if (isBlank(registerDTO.getNombre())) {
	  throw new BadInputException("El nombre es obligatorio");
	}
	if (isBlank(registerDTO.getEmail())) {
	  throw new BadInputException("El email es obligatorio");
	}
	if (isBlank(registerDTO.getPassword())) {
	  throw new BadInputException("La contraseña es obligatoria");
	}
	if (registerDTO.getPassword().length() < 6) {
	  throw new BadInputException("La contraseña debe tener al menos 6 caracteres");
	}
  }

  private AuthResponseDto buildResponse(Usuario usuario, String message) {
	AuthResponseDto response = new AuthResponseDto();
	response.setUserId(usuario.getId());
	response.setNombre(usuario.getNombre());
	response.setEmail(usuario.getEmail());
	response.setToken(generarJwtToken(usuario.getId(), usuario.getEmail()));
	response.setMessage(message);
	return response;
  }

  private boolean isBlank(String value) {
	return value == null || value.trim().isEmpty();
  }


  private String generarJwtToken(Integer userId, String email) {
	Date now = new Date();
	Date expiryDate = new Date(now.getTime() + jwtExpirationMs);

	return Jwts.builder()
		.subject(userId.toString())
		.claim("email", email)
		.issuedAt(now)
		.expiration(expiryDate)
		.signWith(jwtSecretKey)
		.compact();
  }

  public Integer extraerUserIdDelToken(String token) {
	try {
	  Claims claims = extraerClaims(token);
	  String subject = claims.getSubject();
	  return Integer.parseInt(subject);
	} catch (NumberFormatException | JwtException e) {
	  throw new UnauthorizedException("Token invalido");
	}
  }

  public boolean esTokenValido(String token) {
	try {
		extraerClaims(token);
	  return true;
	} catch (JwtException | IllegalArgumentException e) {
	  return false;
	}
  }

  private Claims extraerClaims(String token) {
	return Jwts.parser()
		.verifyWith(jwtSecretKey)
		.build()
		.parseSignedClaims(token)
		.getPayload();
  }
}
