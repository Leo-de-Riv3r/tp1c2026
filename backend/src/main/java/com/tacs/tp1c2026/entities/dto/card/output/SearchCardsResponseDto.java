package com.tacs.tp1c2026.entities.dto.card.output;

import com.tacs.tp1c2026.entities.dto.auction.output.AuctionDto;
import com.tacs.tp1c2026.entities.dto.common.output.PaginationDtoOutput;
import com.tacs.tp1c2026.entities.dto.trade.output.TradePublicationDto;

/**
 * Resultado de la búsqueda de figuritas disponibles: combina las publicaciones activas
 * y las subastas activas que matchean los filtros, cada lista paginada de forma independiente
 * (10 por página por defecto). Se usa desde el Home / SearchPage del FE para mostrar al
 * usuario "dónde puedo conseguir esta figurita" y poder accionar (proponer / ofertar).
 */
public record SearchCardsResponseDto(
    PaginationDtoOutput<TradePublicationDto> publications,
    PaginationDtoOutput<AuctionDto> auctions
) {}
