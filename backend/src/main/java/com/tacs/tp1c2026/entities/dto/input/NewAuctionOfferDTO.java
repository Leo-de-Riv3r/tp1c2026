package com.tacs.tp1c2026.entities.dto.input;

import lombok.Data;
import java.util.List;

@Data
public class NewAuctionOfferDTO {
    private List<ItemsOffer> itemsOffer;

    @Data
    public static class ItemsOffer {
        private Integer figuritaId;
        private Integer cantidad;
    }
}
