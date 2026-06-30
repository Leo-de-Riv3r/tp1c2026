package com.tacs.tp1c2026;

import com.tacs.tp1c2026.entities.auction.Auction;
import com.tacs.tp1c2026.entities.auction.AuctionItem;
import com.tacs.tp1c2026.entities.auction.AuctionOffer;
import com.tacs.tp1c2026.entities.auction.conditions.AuctionCondition;
import com.tacs.tp1c2026.entities.auction.conditions.MinimalCardCount;
import com.tacs.tp1c2026.entities.card.Card;
import com.tacs.tp1c2026.entities.enums.Category;
import com.tacs.tp1c2026.entities.user.User;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Unit tests del algoritmo de selección automática de mejor oferta ({@code Auction.selectBestOffer}),
 * sin Spring/Mongo. Verifica el orden fijo (rareza &gt; cantidad &gt; rating) y la promoción por
 * condición de la subasta.
 */
class AuctionBestOfferTest {

  private int seq = 0;

  // Card sólo tiene @Getter (sin builder ni setters), así que seteamos los campos por reflection.
  // Es test-only y evita tocar la entidad core. Para el ranking sólo importan id, number y category.
  private Card card(Category category) {
    try {
      Card c = new Card();
      setField(c, "id", "card-" + (seq++));
      setField(c, "number", 1);
      setField(c, "category", category);
      return c;
    } catch (ReflectiveOperationException e) {
      throw new RuntimeException(e);
    }
  }

  private void setField(Object target, String name, Object value) throws ReflectiveOperationException {
    Field field = target.getClass().getDeclaredField(name);
    field.setAccessible(true);
    field.set(target, value);
  }

  private AuctionOffer offer(Category category, int amount) {
    AuctionItem item = new AuctionItem(card(category), amount);
    return new AuctionOffer(new User(), List.of(item));
  }

  private Auction auctionWith(List<AuctionCondition> conditions) {
    User publisher = new User();
    publisher.setName("publisher");
    publisher.setAvatarId("avatar_1");
    return new Auction(publisher, card(Category.COMMON), 24, conditions);
  }

  @Test
  void defaultRankingPicksRarestOffer() {
    Auction auction = auctionWith(List.of());
    AuctionOffer epic = offer(Category.EPIC, 5);          // más cartas pero menos raras
    AuctionOffer legendary = offer(Category.LEGENDARY, 1);
    auction.addOffer(epic);
    auction.addOffer(legendary);

    assertSame(legendary, auction.selectBestOffer()); // sin reglas, la rareza manda
  }

  @Test
  void defaultRankingBreaksRarityTieByQuantity() {
    Auction auction = auctionWith(List.of());
    AuctionOffer few = offer(Category.EPIC, 1);
    AuctionOffer many = offer(Category.EPIC, 3);
    auction.addOffer(few);
    auction.addOffer(many);

    assertSame(many, auction.selectBestOffer()); // misma rareza → cantidad desempata
  }

  @Test
  void cardCountConditionPromotesQuantityOverRarity() {
    // Con MIN_CARD_COUNT el subastante prioriza la cantidad por encima de la rareza (el default).
    Auction auction = auctionWith(List.of(new MinimalCardCount(1)));
    AuctionOffer rareButFew = offer(Category.LEGENDARY, 1);
    AuctionOffer commonButMany = offer(Category.COMMON, 3);
    auction.addOffer(rareButFew);
    auction.addOffer(commonButMany);

    assertSame(commonButMany, auction.selectBestOffer()); // cantidad promovida gana
  }
}
