package com.tacs.tp1c2026.entities;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.DocumentReference;

@Document(collection = "ofertas_subasta")
@TypeAlias("ofertaSubasta")
@Getter
@Builder
public class AuctionOffer {
  private String id = UUID.randomUUID().toString();
  @DocumentReference
  private Usuario bidderUser;

  @DocumentReference
  @Builder.Default
  private List<Card> offeredCards = new ArrayList<>();
}
