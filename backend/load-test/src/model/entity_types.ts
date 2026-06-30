export interface CollectionCardDto {
    cardId: string;
    quantity: number;
    compromisedCount: number;
}

export interface AuctionDto {
    id: string;
    cardId: string;
    publisherUserId?: string | null;
    status: string;
    offers?: AuctionOfferDto[];
}

export interface AuctionOfferDto {
    id: string;
    auctionId?: string;
    bidderUserId: string;
    status: string;
    offeredItems?: Array<{ cardId: string; amount: number }>;
}

export interface PublicationDto {
    id: string;
    cardId: string;
    publisherUserId?: string | null;
    status: string;
    remainingCount?: number;
}

export interface ProposalDto {
    id: string;
    publicationId: string;
    status: string;
    proposer?: { id: string };
    receiver?: { id: string };
}

export interface UserBidDto {
    auctionId: string;
    offerId: string;
    offerStatus: string;
}
