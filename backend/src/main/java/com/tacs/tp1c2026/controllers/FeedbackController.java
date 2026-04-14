package com.tacs.tp1c2026.controllers;

import com.tacs.tp1c2026.entities.dto.input.NuevoFeedbackDto;
import com.tacs.tp1c2026.services.FeedbackService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/feedback")
public class FeedbackController {
  private final FeedbackService feedbackService;

  public FeedbackController(FeedbackService feedbackService) {
    this.feedbackService = feedbackService;
  }

  /**
   * {@code POST /api/feedback/} &mdash; Publica una calificación y comentario sobre
   * una publicación de intercambio finalizada.
   *
   * @param userId             identificador del usuario que califica
   * @param nuevoFeedbackDto   datos del feedback: publicación objetivo, calificación y comentario
   * @return 200 OK con mensaje de confirmación
   */
  @PostMapping("/")
  public ResponseEntity<String> createFeedback(@RequestParam Integer userId, @RequestBody NuevoFeedbackDto nuevoFeedbackDto) {
    feedbackService.publicarFeedback(nuevoFeedbackDto, userId);
    return ResponseEntity.ok("Feedback created successfully");  }
}
