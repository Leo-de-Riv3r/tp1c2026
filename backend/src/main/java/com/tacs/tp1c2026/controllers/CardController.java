package com.tacs.tp1c2026.controllers;

import com.tacs.tp1c2026.dto.CardDTO;
import com.tacs.tp1c2026.entities.Figurita;
import com.tacs.tp1c2026.entities.Usuario;
import com.tacs.tp1c2026.services.CardService;
import com.tacs.tp1c2026.services.UserService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
public class CardController {

    private CardService cardService;
    private UserService userService;

    public CardController(CardService cardService) {
        this.cardService = cardService;
    }

    @PostMapping("/{id}/cards")
    public void addCard(@PathVariable Long userId, CardDTO cardDTO) {
        Usuario user = this.userService.getById(userId);
        Figurita figurita = this.cardService.createCard(cardDTO);
    }

}
