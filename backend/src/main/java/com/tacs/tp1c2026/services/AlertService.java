// package com.tacs.tp1c2026.services;

// import java.time.LocalDateTime;
// import java.util.List;

// import com.tacs.tp1c2026.entities.dto.output.AlertaDto;
// import com.tacs.tp1c2026.entities.AlertaFiguritaFaltante;
// import com.tacs.tp1c2026.entities.AlertaPorpuestaRecibida;
// import com.tacs.tp1c2026.entities.AlertaSubastaProxima;
// import com.tacs.tp1c2026.entities.AlertaVisitor;
// import com.tacs.tp1c2026.entities.FiguritaColeccion;
// import com.tacs.tp1c2026.entities.PropuestaIntercambio;
// import com.tacs.tp1c2026.entities.Subasta;
// import com.tacs.tp1c2026.entities.Usuario;
// import com.tacs.tp1c2026.exceptions.UserNotFoundException;
// import com.tacs.tp1c2026.properties.AlertProperties;
// import com.tacs.tp1c2026.repositories.SubastasRepository;
// import com.tacs.tp1c2026.repositories.UsuariosRepository;
// import org.springframework.stereotype.Service;
// import org.springframework.transaction.annotation.Transactional;

// @Service
// public class AlertService {

//     private final UsuariosRepository usuariosRepository;
//     private final SubastasRepository subastasRepository;
//     private final AlertProperties alertProperties;
//     private final AlertaVisitor alertaVisitor;

//     public AlertService(UsuariosRepository usuariosRepository, SubastasRepository subastasRepository, AlertProperties alertProperties, AlertaVisitor alertaVisitor) {
//         this.usuariosRepository = usuariosRepository;
//         this.subastasRepository = subastasRepository;
//         this.alertProperties = alertProperties;
//         this.alertaVisitor = alertaVisitor;
//     }

//     /**
//      * Notifica a todos los usuarios que tienen a una figurita como faltante que hay
//      * un nuevo usuario con esa figurita disponible para intercambio.
//      * No notifica al propio usuario que la publicó.
//      *
//      * @param usuario           usuario que posee la figurita disponible
//      * @param figuritaColeccion figurita de la colección que origina la notificación
//      */
//     public void notificarUsuariosDeFigurita(Usuario usuario, FiguritaColeccion figuritaColeccion) {

//         List<Usuario> usuariosFaltantes = figuritaColeccion.getFigurita().getUsuariosFaltantes();
//         for (Usuario u : usuariosFaltantes) {
//             if (!u.getId().equals(usuario.getId())) {
//                 u.agregarAlerta(new AlertaFiguritaFaltante(usuario, figuritaColeccion.getFigurita()));
//             }
//         }

//     }

//     /**
//      * Notifica al dueño de una publicación que recibió una nueva propuesta de intercambio.
//      *
//      * @param propuesta propuesta de intercambio recibida
//      */
//     public void notificarPropuestaRecibida(PropuestaIntercambio propuesta) {
//         Usuario receptor = propuesta.getPublicacion().getPublicante();
//         Usuario proponente = propuesta.getUsuario();
//         receptor.agregarAlerta(new AlertaPorpuestaRecibida(proponente, propuesta));
//     }

//     /**
//      * Genera alertas de subasta próxima para todos los usuarios interesados en subastas
//      * cuyo cierre ocurre dentro del umbral configurado en {@link AlertProperties}.
//      * Se ejecuta de forma transaccional.
//      */
//     @Transactional
//     public void alertarSubastasProximas() {
//         LocalDateTime now = LocalDateTime.now();

//         List<Subasta> subastasProximas = subastasRepository.findAll().stream()
//             .filter(subasta -> now.plusMinutes(alertProperties.getWarningThresholdMinutes()).isAfter(subasta.getFechaCierre()) && now.isBefore(subasta.getFechaCierre()))
//             .toList();

//         for (Subasta subasta : subastasProximas) {
//             subasta.getUsuariosInteresados().forEach(usuario -> {
//                 usuario.agregarAlerta(new AlertaSubastaProxima(usuario, subasta));
//             });
//         }
//     }

//     /**
//      * Retorna todas las alertas pendientes del usuario, mapeadas a DTOs.
//      *
//      * @param userId identificador del usuario
//      * @return lista de {@link AlertaDto} con las alertas del usuario; lista vacía si no tiene alertas
//      * @throws UserNotFoundException si el usuario no existe
//      */
//     @Transactional(readOnly = true)
//     public List<AlertaDto> obtenerAlertasUsuario(Integer userId) {
//         Usuario usuario = usuariosRepository.findById(userId)
//             .orElseThrow(() -> new UserNotFoundException("No se encontro el usuario"));

//         return usuario.getAlertas() == null ? List.of() : usuario.getAlertas().stream()
//             .map(alerta -> alerta.visit(alertaVisitor))
//             .toList();
//     }
// }
