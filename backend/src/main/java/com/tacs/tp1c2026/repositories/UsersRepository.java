package com.tacs.tp1c2026.repositories;

import com.tacs.tp1c2026.entities.Usuario;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.Optional;
import java.util.stream.Stream;

public interface UsersRepository extends MongoRepository<Usuario, String> {

    Optional<Usuario> findByEmail(String email);
    boolean existsByEmail(String email);
    Stream<Usuario> findAllBy();

    /* Commands sobre la colección del usuario */

    /* Incrementa la cantidad de una card ya existente */
//    @Query("{ '_id': ?0, 'collection.card.id': ?1 }") // x las dudas, esto significa "WHERE id = userId AND collection.card.id = figuritaId"
//    @Update("{ '$inc': { 'collection.$.quantity': 1 } }") // sería como un "set campo =  campo + 1 para el update"
//    long incrementCollectionItemQuantity(String userId, Integer figuritaId);
//
//    /* Agrega un nuevo sucdoc */
//    @Query("{ '_id': ?0 }") // WHERE id = userId
//    @Update("{ '$push': { 'collection': ?1 } }") // insert en la collection del user la card fc
//    void pushToCollection(String userId, CardCollection fc);
//
//    /* decrementa la cant de una card solo si ya tenia mas de un item, sino retorna cero asi el service elimina el subdoc de la coleccion */
//    @Query("{ '_id': ?0, 'collection.card.id': ?1, 'collection.quantity': { '$gt': 1 } }") // where id = userId y collection tiene a figuritaid y la card tiene quantity > 1
//    @Update("{ '$inc': { 'collection.$.quantity': -1 } }") // set quantity = quantity -1
//    long decrementCollectionQuantity(String userId, Integer figuritaId);
//
//    /* elimino completamente una card de la coleccion porque quantity llegó a 0 */
//    @Query("{ '_id': ?0 }") // WHERE id = userId
//    @Update("{ '$pull': { 'collection': { 'card.id': ?1 } } }") // delete el subdoc con ese figuritaId de la coleccion
//    void pullFromCollection(String userId, Integer figuritaId);
//
//    /* Commands sobre los faltantes del usuario */
//
//    /* insert en faltantes solo si no existe */
//    @Query("{ '_id': ?0, 'missingCards.card.id': { '$ne': ?1 } }") // where id = userid y figuritaId no existe en missingCards (el ne)
//    @Update("{ '$push': { 'missingCards': ?2 } }") // si se cumple, agrega a missingcards la card faltante ff
//    long pushToMissingCards(String userId, Integer figuritaId, FiguritaFaltante ff);
//
//    /* saco una card de faltantes, si el usuario ya la consigue por una subasta o publicacion */
//    @Query("{ '_id': ?0 }")
//    @Update("{ '$pull': { 'missingCards': { 'card.id': ?1 } } }") // idem que en el de la coleccion
//    void pullFromMissingCards(Integer userId, Integer figuritaId);


}
