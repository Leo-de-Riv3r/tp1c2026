 package com.tacs.tp1c2026.services;

 import com.tacs.tp1c2026.repositories.AuctionRepository;
 import java.time.LocalDateTime;
 import java.util.List;

 import com.tacs.tp1c2026.entities.dto.output.AlertaDto;
 import com.tacs.tp1c2026.entities.AlertaPropuestaRecibida;
 import com.tacs.tp1c2026.entities.AlertaSubastaProxima;
 import com.tacs.tp1c2026.entities.AlertaVisitor;
 import com.tacs.tp1c2026.entities.ExchangeProposal;
 import com.tacs.tp1c2026.entities.Auction;
 import com.tacs.tp1c2026.entities.Usuario;
 import com.tacs.tp1c2026.exceptions.UserNotFoundException;
 import com.tacs.tp1c2026.properties.AlertProperties;
 import com.tacs.tp1c2026.repositories.UsersRepository;
 import org.springframework.beans.factory.annotation.Autowired;
 import org.springframework.stereotype.Service;
 import org.springframework.transaction.annotation.Transactional;

 @Service
 public class AlertService {
   @Autowired
     private UsersRepository usuariosRepository;
   @Autowired
     private AuctionRepository subastasRepository;
     @Autowired
     private AlertProperties alertProperties;
     @Autowired
     private AlertaVisitor alertaVisitor;


//     /**
//      * Notifica a todos los usuarios que tienen a una card como faltante que hay
//      * un nuevo usuario con esa card disponible para intercambio.
//      * No notifica al propio usuario que la publicó.
//      *
//      * @param usuario           usuario que posee la card disponible
//      * @param figuritaColeccion card de la colección que origina la notificación
//      */
//     public void notificarUsuariosDeFigurita(Usuario usuario, CardCollection figuritaColeccion) {
//
//         List<Usuario> usuariosFaltantes = figuritaColeccion.getCard().getUsuariosFaltantes();
//         for (Usuario u : usuariosFaltantes) {
//             if (!u.getId().equals(usuario.getId())) {
//                 u.agregarAlerta(new AlertaFiguritaFaltante(usuario, figuritaColeccion.getCard()));
//             }
//         }
//
//     }

     /**
      * Notifica al dueño de una publicación que recibió una nueva propuesta de intercambio.
      *
      * @param proposal propuesta de intercambio recibida
      */
     public void notificarPropuestaRecibida(ExchangeProposal proposal) {
         Usuario receptor = proposal.getPublication().getPublisherUser();
         receptor.addAlert(new AlertaPropuestaRecibida(proposal));
         usuariosRepository.save(receptor);
     }

     /**
      * Genera alertas de subasta próxima para todos los usuarios interesados en subastas
      * cuyo cierre ocurre dentro del umbral configurado en {@link AlertProperties}.
      * Se ejecuta de forma transaccional.
      */
     @Transactional
     public void alertarSubastasProximas() {
         LocalDateTime now = LocalDateTime.now();

         List<Auction> subastasProximas = subastasRepository.findAll().stream()
             .filter(subasta -> now.plusMinutes(alertProperties.getWarningThresholdMinutes()).isAfter(subasta.getFinishDate()) && now.isBefore(subasta.getFinishDate()))
             .toList();

         for (Auction subasta : subastasProximas) {
             subasta.getUsuariosInteresados().forEach(usuario -> {
                 usuario.addAlert(new AlertaSubastaProxima(subasta.getId(), subasta.getCard(), subasta.getFinishDate()));
             });
         }
     }

     /**
      * Retorna todas las alertas pendientes del usuario, mapeadas a DTOs.
      *
      * @param userId identificador del usuario
      * @return lista de {@link AlertaDto} con las alertas del usuario; lista vacía si no tiene alertas
      * @throws UserNotFoundException si el usuario no existe
      */
     @Transactional(readOnly = true)
     public List<AlertaDto> obtenerAlertasUsuario(String userId) {
         Usuario usuario = usuariosRepository.findById(userId)
             .orElseThrow(() -> new UserNotFoundException("No se encontro el usuario"));

         return usuario.getAlert() == null ? List.of() : usuario.getAlert().stream()
             .map(alerta -> alerta.visit(alertaVisitor))
             .toList();
     }
 }
