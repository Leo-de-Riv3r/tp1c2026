package com.tacs.tp1c2026.repositories;

import com.tacs.tp1c2026.entities.Usuario;
import com.tacs.tp1c2026.entities.embedded.FiguritaColeccion;
import com.tacs.tp1c2026.entities.embedded.FiguritaFaltante;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.Update;
import java.util.Optional;
import java.util.stream.Stream;

public interface UsuariosRepository extends MongoRepository<Usuario, String> {

    Optional<Usuario> findByEmail(String email);
    boolean existsByEmail(String email);
    Stream<Usuario> findAllBy();

    /* Commands sobre la colección del usuario */

    /* Incrementa la cantidad de una figurita ya existente */
    @Query("{ '_id': ?0, 'collection.figuritaId': ?1 }") // x las dudas, esto significa "WHERE id = userId AND collection.figuritaId = figuritaId"
    @Update("{ '$inc': { 'collection.$.quantity': 1 } }") // sería como un "set campo =  campo + 1 para el update"
    long incrementCollectionItemQuantity(String userId, String figuritaId);

    /* Agrega un nuevo sucdoc */
    @Query("{ '_id': ?0 }") // WHERE id = userId
    @Update("{ '$push': { 'collection': ?1 } }") // insert en la collection del user la figurita fc
    void pushToCollection(String userId, FiguritaColeccion fc);

    /* decrementa la cant de una figurita solo si ya tenia mas de un item, sino retorna cero asi el service elimina el subdoc de la coleccion */
    @Query("{ '_id': ?0, 'collection.figuritaId': ?1, 'collection.quantity': { '$gt': 1 } }") // where id = userId y collection tiene a figuritaid y la figurita tiene quantity > 1
    @Update("{ '$inc': { 'collection.$.quantity': -1 } }") // set quantity = quantity -1
    long decrementCollectionQuantity(String userId, String figuritaId);

    /* elimino completamente una figurita de la coleccion porque quantity llegó a 0 */
    @Query("{ '_id': ?0 }") // WHERE id = userId
    @Update("{ '$pull': { 'collection': { 'figuritaId': ?1 } } }") // delete el subdoc con ese figuritaId de la coleccion
    void pullFromCollection(String userId, String figuritaId);

    /* Commands sobre los faltantes del usuario */

    /* insert en faltantes solo si no existe */
    @Query("{ '_id': ?0, 'missingCards.figuritaId': { '$ne': ?1 } }") // where id = userid y figuritaId no existe en missingCards (el ne)
    @Update("{ '$push': { 'missingCards': ?2 } }") // si se cumple, agrega a missingcards la figurita faltante ff
    long pushToMissingCards(String userId, String figuritaId, FiguritaFaltante ff);

    /* saco una figurita de faltantes, si el usuario ya la consigue por una subasta o publicacion */
    @Query("{ '_id': ?0 }")
    @Update("{ '$pull': { 'missingCards': { 'figuritaId': ?1 } } }") // idem que en el de la coleccion
    void pullFromMissingCards(String userId, String figuritaId);
}
