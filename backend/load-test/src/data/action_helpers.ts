import { API } from "../model/api.js";
import { CollectionCardDto, AuctionDto, PublicationDto, ProposalDto } from "../model/entity_types.js";
import { VUser } from "../model/vuser.js";

export function pickRandom<T>(items: T[]): T | null {
    if (items.length === 0) return null;
    return items[Math.floor(Math.random() * items.length)];
}

export function availableQuantity(card: CollectionCardDto): number {
    return card.quantity - card.compromisedCount;
}

export async function ensureCollection(api: API, vuser: VUser): Promise<CollectionCardDto[]> {
    if (!vuser.userId) return [];
    if (vuser.collection.length === 0) {
        vuser.collection = await api.getUserCollection(vuser, vuser.userId);
    }
    return vuser.collection as CollectionCardDto[];
}

export function pickTradableCard(cards: CollectionCardDto[]): string | null {
    const available = cards.filter((c) => availableQuantity(c) > 0);
    return pickRandom(available)?.cardId ?? null;
}

export function pickTradableCards(cards: CollectionCardDto[], maxCards: number): Array<{ cardId: string; amount: number }> {
    const available = cards.filter((c) => availableQuantity(c) > 0);
    const picked = available.slice(0, Math.min(maxCards, available.length));
    return picked.map((c) => ({ cardId: c.cardId, amount: 1 }));
}

export function commitCardsLocally(vuser: VUser, items: Array<{ cardId: string; amount: number }>): void {
    const cards = vuser.collection as CollectionCardDto[];
    for (const item of items) {
        const card = cards.find((c) => c.cardId === item.cardId);
        if (card) card.compromisedCount += item.amount;
    }
}

export function asAuction(value: unknown): AuctionDto | null {
    if (!value || typeof value !== "object") return null;
    const a = value as AuctionDto;
    return a.id ? a : null;
}

export function asPublication(value: unknown): PublicationDto | null {
    if (!value || typeof value !== "object") return null;
    const p = value as PublicationDto;
    return p.id ? p : null;
}

export function asProposal(value: unknown): ProposalDto | null {
    if (!value || typeof value !== "object") return null;
    const p = value as ProposalDto;
    return p.id ? p : null;
}

export function auctionPublisherId(auction: AuctionDto): string | null {
    return auction.publisherUserId ?? null;
}

export function publicationPublisherId(publication: PublicationDto): string | null {
    return publication.publisherUserId ?? null;
}

export function isActiveAuction(auction: AuctionDto): boolean {
    return auction.status === "ACTIVE";
}

export function isActivePublication(publication: PublicationDto): boolean {
    return publication.status === "ACTIVE";
}

export function isPendingProposal(proposal: ProposalDto): boolean {
    return proposal.status === "PENDING";
}

export function pickPendingReceivedProposal(vuser: VUser): ProposalDto | null {
    const pending = (vuser.proposals as ProposalDto[]).filter(isPendingProposal);
    return pickRandom(pending);
}

export function pickPendingSentProposal(vuser: VUser): ProposalDto | null {
    const pending = (vuser.proposals as ProposalDto[]).filter(isPendingProposal);
    return pickRandom(pending);
}

export function pickPendingOfferOnOwnedAuction(vuser: VUser): { auctionId: string; offerId: string } | null {
    const auction = asAuction(vuser.selectedAuction);
    if (!auction || auctionPublisherId(auction) !== vuser.userId) return null;
    const offers = (auction.offers ?? []).filter((o) => o.status === "PENDING");
    const offer = pickRandom(offers);
    if (!offer) return null;
    return { auctionId: auction.id, offerId: offer.id };
}

export function pickOwnPendingBid(vuser: VUser): { auctionId: string; offerId: string } | null {
    const bids = vuser.myBids as Array<{ auctionId: string; offerId?: string; bidId?: string; offerStatus?: string }>;
    const pending = bids.filter((b) => (b.offerStatus ?? "PENDING") === "PENDING");
    const bid = pickRandom(pending);
    if (!bid) return null;
    return { auctionId: bid.auctionId, offerId: bid.offerId ?? bid.bidId ?? "" };
}

export function ownsSelectedAuction(vuser: VUser): boolean {
    const auction = asAuction(vuser.selectedAuction);
    return auction !== null && auctionPublisherId(auction) === vuser.userId;
}

export function ownsSelectedPublication(vuser: VUser): boolean {
    const publication = asPublication(vuser.selectedPublication);
    return publication !== null && publicationPublisherId(publication) === vuser.userId;
}
