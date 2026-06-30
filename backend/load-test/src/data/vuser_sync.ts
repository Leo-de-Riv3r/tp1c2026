import { AuctionDto, AuctionOfferDto, ProposalDto, PublicationDto } from "../model/entity_types.js";
import { VUser } from "../model/vuser.js";
import { asAuction, asProposal, asPublication } from "./action_helpers.js";

export function findVUserByUserId(users: VUser[], userId: string): VUser | undefined {
    return users.find((u) => u.userId === userId && u.isLoggedIn());
}

export function upsertById<T extends { id: string }>(list: unknown[], item: T): void {
    const arr = list as T[];
    const idx = arr.findIndex((x) => x.id === item.id);
    if (idx >= 0) arr[idx] = item;
    else arr.push(item);
}

export function upsertAuction(vuser: VUser, auction: unknown): void {
    const parsed = asAuction(auction);
    if (!parsed) return;
    upsertById(vuser.auctions, parsed);
    if (vuser.selectedAuctionId === parsed.id) {
        vuser.selectedAuction = auction;
    }
}

export function upsertPublication(vuser: VUser, publication: unknown): void {
    const parsed = asPublication(publication);
    if (!parsed) return;
    upsertById(vuser.publications, publication as PublicationDto);
    if (vuser.selectedPublicationId === parsed.id) {
        vuser.selectedPublication = publication;
    }
}

export function upsertProposal(vuser: VUser, proposal: unknown): void {
    const parsed = asProposal(proposal);
    if (!parsed) return;
    upsertById(vuser.proposals, parsed);
}

export function syncProposalBetweenUsers(
    users: VUser[],
    proposal: unknown,
    publisherId: string,
    proposerId: string,
): void {
    const publisher = findVUserByUserId(users, publisherId);
    const proposer = findVUserByUserId(users, proposerId);
    if (publisher) upsertProposal(publisher, proposal);
    if (proposer) upsertProposal(proposer, proposal);
}

export function syncAuctionBetweenUsers(users: VUser[], auction: unknown, publisherId: string): void {
    const publisher = findVUserByUserId(users, publisherId);
    if (publisher) upsertAuction(publisher, auction);
}

export function syncOfferPlaced(
    users: VUser[],
    bidder: VUser,
    auctionId: string,
    publisherId: string,
    offer: unknown,
    refreshedAuction: unknown,
): void {
    const offerDto = offer as AuctionOfferDto;
    const bidEntry = {
        auctionId,
        offerId: offerDto.id,
        offerStatus: offerDto.status ?? "PENDING",
    };
    const bids = bidder.myBids as Array<{ auctionId: string; offerId: string; offerStatus: string }>;
    const idx = bids.findIndex((b) => b.offerId === offerDto.id);
    if (idx >= 0) bids[idx] = bidEntry;
    else bids.push(bidEntry);

    const owner = findVUserByUserId(users, publisherId);
    if (owner) {
        upsertAuction(owner, refreshedAuction);
    }
    if (bidder.selectedAuctionId === auctionId) {
        bidder.selectedAuction = refreshedAuction;
    }
}

export function markProposalStatus(vuser: VUser, proposalId: string, status: string): void {
    const proposals = vuser.proposals as ProposalDto[];
    const proposal = proposals.find((p) => p.id === proposalId);
    if (proposal) proposal.status = status;
}

export function syncProposalStatusBetweenUsers(
    users: VUser[],
    proposalId: string,
    publisherId: string,
    proposerId: string,
    status: string,
): void {
    const publisher = findVUserByUserId(users, publisherId);
    const proposer = findVUserByUserId(users, proposerId);
    if (publisher) markProposalStatus(publisher, proposalId, status);
    if (proposer) markProposalStatus(proposer, proposalId, status);
}

export async function refreshUserExchanges(api: import("../model/api.js").API, vuser: VUser): Promise<void> {
    if (!vuser.userId) return;
    vuser.exchanges = await api.getExchangesByUserId(vuser, vuser.userId);
}

export async function refreshUserCollection(api: import("../model/api.js").API, vuser: VUser): Promise<void> {
    if (!vuser.userId) return;
    vuser.collection = await api.getUserCollection(vuser, vuser.userId);
}
