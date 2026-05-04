package com.tacs.tp1c2026.entities.user.embebbed;

import com.tacs.tp1c2026.entities.card.Card;
import com.tacs.tp1c2026.entities.user.User;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DocumentReference;

@Getter
public class Suggestion {

  private String id = new ObjectId().toHexString();

  @DocumentReference
  private User suggestedUser;

  @DocumentReference
  private final List<Card> obtainableCards = new ArrayList<>();

  public Suggestion(User suggestedUser, List<Card> obtainableCards) {
    this.suggestedUser = suggestedUser;
    this.obtainableCards.addAll(obtainableCards);
  }
}
