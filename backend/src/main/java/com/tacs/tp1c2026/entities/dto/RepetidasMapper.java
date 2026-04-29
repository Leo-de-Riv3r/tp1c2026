//package com.tacs.tp1c2026.entities.dto;
//
//import com.tacs.tp1c2026.entities.CardCollection;
//import com.tacs.tp1c2026.entities.dto.output.FiguritaDto;
//import com.tacs.tp1c2026.entities.dto.output.RepeatedCardDto;
//import java.util.List;
//import org.springframework.stereotype.Component;
//
//@Component
//public class RepetidasMapper {
//
//    /**
//     * Convierte una lista de {@link CardCollection} a una lista de {@link RepeatedCardDto},
//     * incluyendo la cantidad y los datos descriptivos de cada card.
//     *
//     * @param repetidas lista de entidades de card en colección
//     * @return lista de DTOs correspondientes
//     */
//    public List<RepeatedCardDto> toDTOList(List<CardCollection> repetidas) {
//        return repetidas.stream()
//            .map(figuritaColeccion -> {
//                FiguritaDto figuritaDto = new FiguritaDto(
//                    figuritaColeccion.getCard().getNumero(),
//                    figuritaColeccion.getCard().getDescripcion(),
//                    figuritaColeccion.getCard().getJugador(),
//                    figuritaColeccion.getCard().getSeleccion(),
//                    figuritaColeccion.getCard().getEquipo(),
//                    figuritaColeccion.getCard().getCategoria()
//                );
//                return new RepeatedCardDto(figuritaColeccion.getCantidadLibre(), figuritaDto);
//            }).toList();
//    }
//}
