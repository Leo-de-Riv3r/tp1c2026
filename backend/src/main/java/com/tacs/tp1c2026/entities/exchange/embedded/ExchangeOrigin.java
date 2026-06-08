package com.tacs.tp1c2026.entities.exchange.embedded;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.TypeAlias;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@TypeAlias("exchange_origin")
public class ExchangeOrigin {

    public enum Type { PROPUESTA, SUBASTA }

    private Type type;
    private String id;

    public static ExchangeOrigin fromProposal(String proposalId) {
        return new ExchangeOrigin(Type.PROPUESTA, proposalId);
    }

    public static ExchangeOrigin fromAuction(String auctionId) {
        return new ExchangeOrigin(Type.SUBASTA, auctionId);
    }
}
