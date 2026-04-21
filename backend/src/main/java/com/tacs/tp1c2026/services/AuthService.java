package com.tacs.tp1c2026.services;

import com.tacs.tp1c2026.entities.Usuario;
import com.tacs.tp1c2026.entities.dto.input.LoginDTO;
import com.tacs.tp1c2026.entities.dto.input.RegisterDTO;
import com.tacs.tp1c2026.entities.dto.output.LoginResponseDto;
import com.tacs.tp1c2026.entities.dto.output.UserDto;
import com.tacs.tp1c2026.exceptions.BadInputException;
import com.tacs.tp1c2026.exceptions.ConflictException;
import com.tacs.tp1c2026.exceptions.UnauthorizedException;
import com.tacs.tp1c2026.repositories.UsuariosRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.time.LocalDateTime;
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
  private final String adminEmail;
  private final String adminPassword;
  private final String adminPasswordHash;

  public AuthService(UsuariosRepository usuariosRepository,
					 @Value("${jwt.secret}") String jwtSecret,
					 @Value("${jwt.expiration}") long jwtExpirationMs,
					 @Value("${admin.email:admin@tacs.local}") String adminEmail,
					 @Value("${admin.password:}") String adminPassword,
					 @Value("${admin.password-hash:}") String adminPasswordHash) {
	this.usuariosRepository = usuariosRepository;
	this.jwtSecretKey = Keys.hmacShaKeyFor(jwtSecret.getBytes());
	this.jwtExpirationMs = jwtExpirationMs;
	this.passwordEncoder = new BCryptPasswordEncoder();
	this.adminEmail = adminEmail == null ? "" : adminEmail.trim().toLowerCase();
	this.adminPassword = adminPassword;
	this.adminPasswordHash = adminPasswordHash;
  }


  public UserDto register(RegisterDTO registerDTO) {
	validarRegistro(registerDTO);
	String email = registerDTO.getEmail().trim().toLowerCase();

	if (usuariosRepository.existsByEmail(email)) {
	  throw new ConflictException("El email ya se encuentra registrado");
	}

	Usuario nuevoUsuario = new Usuario();
	nuevoUsuario.setName(registerDTO.getName().trim());
	nuevoUsuario.setEmail(email);
	nuevoUsuario.setAvatarId(registerDTO.getAvatarId().trim());
	nuevoUsuario.setPasswordHash(passwordEncoder.encode(registerDTO.getPassword()));
	nuevoUsuario.setCreationDate(LocalDateTime.now());

	Usuario usuarioCreado = usuariosRepository.save(nuevoUsuario);
	return mapUser(usuarioCreado);
  }

  public LoginResponseDto login(LoginDTO loginDTO) {
	if (loginDTO == null || isBlank(loginDTO.getEmail()) || isBlank(loginDTO.getPassword())) {
	  throw new BadInputException("Email y password son obligatorios");
	}

	String email = loginDTO.getEmail().trim().toLowerCase();

	Usuario usuario = usuariosRepository.findByEmail(email)
		.orElseThrow(() -> new UnauthorizedException("Credenciales invalidas"));


	if (usuario.getPasswordHash() == null || !passwordEncoder.matches(loginDTO.getPassword(), usuario.getPasswordHash())) {
	  throw new UnauthorizedException("Credenciales invalidas");
	}

	usuario.setLastLogin(LocalDateTime.now());
	usuariosRepository.save(usuario);

	return buildResponse(usuario);
  }

  public LoginResponseDto adminLogin(LoginDTO loginDTO) {
	if (loginDTO == null || isBlank(loginDTO.getEmail()) || isBlank(loginDTO.getPassword())) {
	  throw new BadInputException("Email y password son obligatorios");
	}

	if (isBlank(adminPasswordHash) && isBlank(adminPassword)) {
	  throw new UnauthorizedException("Admin no configurado");
	}

	String emailAdmin = loginDTO.getEmail().trim().toLowerCase();
	boolean contraValida = !isBlank(adminPasswordHash) ? passwordEncoder.matches(loginDTO.getPassword(), adminPasswordHash) : loginDTO.getPassword().equals(adminPassword);

	if (!emailAdmin.equals(adminEmail) || !contraValida) {
	  throw new UnauthorizedException("Credenciales invalidas");
	}

	LoginResponseDto response = new LoginResponseDto();
	response.setToken(generarJwtToken("admin", emailAdmin, "ADMIN"));
	response.setUser(mapAdminUser(emailAdmin));
	return response;
  }

  private void validarRegistro(RegisterDTO registerDTO) {
	if (registerDTO == null) {
	  throw new BadInputException("Request de registro invalido");
	}
	if (isBlank(registerDTO.getName())) {
	  throw new BadInputException("El nombre es obligatorio");
	}
	if (isBlank(registerDTO.getEmail())) {
	  throw new BadInputException("El email es obligatorio");
	}
	if (isBlank(registerDTO.getPassword())) {
	  throw new BadInputException("La contraseña es obligatoria");
	}
	if (isBlank(registerDTO.getAvatarId())) {
	  throw new BadInputException("El avatar es obligatorio");
	}
	if (registerDTO.getPassword().length() < 6) {
	  throw new BadInputException("La contraseña debe tener al menos 6 caracteres");
	}
  }

  private LoginResponseDto buildResponse(Usuario usuario) {
	LoginResponseDto response = new LoginResponseDto();
	response.setToken(generarJwtToken(usuario.getId(), usuario.getEmail(), "USER"));
	response.setUser(mapUser(usuario));
	return response;
  }

  private UserDto mapAdminUser(String email) {
	UserDto admin = new UserDto();
	admin.setId("admin");
	admin.setName("Administrador");
	admin.setEmail(email);
	admin.setRating(null);
	admin.setExchangesAmount(0);
	admin.setAvatarId("admin");
	admin.setCreationDate(LocalDateTime.now());
	return admin;
  }

  private UserDto mapUser(Usuario usuario) {
	  UserDto user = new UserDto();
	user.setId(usuario.getId());
	user.setName(usuario.getName());
	user.setEmail(usuario.getEmail());
	user.setRating(usuario.getRating());
	user.setExchangesAmount(usuario.getExchangesCount());
	user.setAvatarId(usuario.getAvatarId());
	user.setCreationDate(usuario.getCreationDate());
	return user;
  }

  private boolean isBlank(String value) {
	return value == null || value.trim().isEmpty();
  }


  private String generarJwtToken(String userId, String email, String role) {
	Date now = new Date();
	Date expiryDate = new Date(now.getTime() + jwtExpirationMs);

	return Jwts.builder()
		.subject(userId)
		.claim("email", email)
	.claim("role", role)
		.issuedAt(now)
		.expiration(expiryDate)
		.signWith(jwtSecretKey)
		.compact();
  }

  public String extraerRoleDelToken(String token) {
	try {
	  Claims claims = extraerClaims(token);
	  String role = claims.get("role", String.class);
	  return isBlank(role) ? "USER" : role;
	} catch (JwtException e) {
	  throw new UnauthorizedException("Token invalido");
	}
  }

  public String extraerUserIdDelToken(String token) {
	try {
	  Claims claims = extraerClaims(token);
	  String subject = claims.getSubject();
	  if (subject == null || subject.trim().isEmpty()) {
		throw new UnauthorizedException("Token invalido");
	  }
	  return subject;
	} catch (JwtException e) {
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
