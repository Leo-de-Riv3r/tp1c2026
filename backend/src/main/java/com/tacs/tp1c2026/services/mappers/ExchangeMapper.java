package com.tacs.tp1c2026.services.mappers;

import com.tacs.tp1c2026.entities.dto.exchange.output.ExchangeDto;
import com.tacs.tp1c2026.entities.exchange.Exchange;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ExchangeMapper {

  public ExchangeDto mapExchange(Exchange exchange) {
    return new ExchangeDto(
        exchange.getId(),
        exchange.getOrigin(),
        exchange.getUserA(),
        exchange.getUserB(),
        exchange.getCardsFromA(),
        exchange.getCardsFromB(),
        exchange.getStatus(),
        exchange.getCreatedAt(),
        exchange.getFeedbackFromA(),
        exchange.getFeedbackFromB()
    );
  }

  public List<ExchangeDto> mapExchanges(List<Exchange> exchanges) {
    return exchanges.stream().map(this::mapExchange).toList();
  }
}
