import { API } from "../model/api.js";
import { Transition } from "../model/transition.js";
import { VUser } from "../model/vuser.js";
import {
    asAuction,
    asProposal,
    asPublication,
    auctionPublisherId,
    commitCardsLocally,
    ensureCollection,
    isActiveAuction,
    isActivePublication,
    ownsSelectedAuction,
    ownsSelectedPublication,
    pickOwnPendingBid,
    pickPendingOfferOnOwnedAuction,
    pickPendingReceivedProposal,
    pickPendingSentProposal,
    pickTradableCard,
    pickTradableCards,
    publicationPublisherId,
} from "./action_helpers.js";
import { getState } from "./state_registry.js";
import { StateIds } from "./state_ids.js";
import {
    findVUserByUserId,
    refreshUserCollection,
    refreshUserExchanges,
    syncAuctionBetweenUsers,
    syncOfferPlaced,
    syncProposalBetweenUsers,
    syncProposalStatusBetweenUsers,
    upsertAuction,
    upsertPublication,
} from "./vuser_sync.js";

function unwrapData<T>(value: unknown): T | null {
    if (!value || typeof value !== "object") return null;
    if ("data" in value) return (value as { data: T }).data;
    return value as T;
}

export const CREATE_AUCTION = new Transition(
    "CREATE_AUCTION",
    async (api, vuser, users) => {
        const cards = await ensureCollection(api, vuser);
        const cardId = pickTradableCard(cards);
        if (!cardId || !vuser.userId) return vuser.state;

        const created = await api.createAuction(vuser, {
            cardId,
            auctionDurationHours: 24,
        });
        commitCardsLocally(vuser, [{ cardId, amount: 1 }]);

        const auctionData = unwrapData<unknown>(created) ?? created;
        const auction = asAuction(auctionData);
        if (auction) {
            vuser.selectedAuctionId = auction.id;
            vuser.selectedAuction = auctionData;
            upsertAuction(vuser, auctionData);
            syncAuctionBetweenUsers(users, auctionData, vuser.userId);
            vuser.previousStateId = vuser.state.id;
            return getState(StateIds.AUCTION_DETAIL);
        }
        return getState(StateIds.AUCTIONS_MY);
    },
    async (api, vuser) => {
        if (!vuser.isLoggedIn()) return false;
        const cards = await ensureCollection(api, vuser);
        return pickTradableCard(cards) !== null;
    },
);

export const PUBLISH_CARD = new Transition(
    "PUBLISH_CARD",
    async (api, vuser) => {
        const cards = await ensureCollection(api, vuser);
        const cardId = pickTradableCard(cards);
        if (!cardId) return vuser.state;

        const created = await api.publishFigurita(vuser, { cardId, quantity: 1 });
        commitCardsLocally(vuser, [{ cardId, amount: 1 }]);
        upsertPublication(vuser, created);
        return vuser.state;
    },
    async (api, vuser) => {
        if (!vuser.isLoggedIn()) return false;
        const cards = await ensureCollection(api, vuser);
        return pickTradableCard(cards) !== null;
    },
);

export const PLACE_BID = new Transition(
    "PLACE_BID",
    async (api, vuser, users) => {
        const auction = asAuction(vuser.selectedAuction);
        if (!auction || !vuser.userId || auctionPublisherId(auction) === vuser.userId) return vuser.state;
        if (!isActiveAuction(auction)) return vuser.state;

        const cards = await ensureCollection(api, vuser);
        const items = pickTradableCards(cards, 1);
        if (items.length === 0) return vuser.state;

        const offer = await api.placeBid(vuser, auction.id, items);
        commitCardsLocally(vuser, items);

        const refreshed = await api.getAuctionById(vuser, auction.id);
        vuser.selectedAuction = refreshed;
        const publisherId = auctionPublisherId(auction);
        if (publisherId) {
            syncOfferPlaced(users, vuser, auction.id, publisherId, offer, refreshed);
        }
        return vuser.state;
    },
    async (api, vuser) => {
        if (!vuser.isLoggedIn() || ownsSelectedAuction(vuser)) return false;
        const auction = asAuction(vuser.selectedAuction);
        if (!auction || !isActiveAuction(auction)) return false;
        const cards = await ensureCollection(api, vuser);
        return pickTradableCards(cards, 1).length > 0;
    },
);

export const MAKE_PROPOSAL = new Transition(
    "MAKE_PROPOSAL",
    async (api, vuser, users) => {
        const publication = asPublication(vuser.selectedPublication);
        if (!publication || !vuser.userId || publicationPublisherId(publication) === vuser.userId) return vuser.state;
        if (!isActivePublication(publication)) return vuser.state;

        const cards = await ensureCollection(api, vuser);
        const cardIds = pickTradableCards(cards, 1).map((i) => i.cardId);
        if (cardIds.length === 0) return vuser.state;

        const created = await api.makeProposal(vuser, {
            publicationId: publication.id,
            cardIds,
            requestedCount: 1,
        });
        commitCardsLocally(vuser, cardIds.map((id) => ({ cardId: id, amount: 1 })));

        const proposalData = unwrapData<unknown>(created) ?? created;
        const proposal = asProposal(proposalData);
        const publisherId = publicationPublisherId(publication);
        if (proposal && publisherId) {
            const proposerId = proposal.proposer?.id ?? vuser.userId;
            syncProposalBetweenUsers(users, proposalData, publisherId, proposerId);
            if (vuser.selectedPublicationId === publication.id) {
                vuser.proposals = await api.getProposalsByPublicationId(vuser, publication.id);
            }
        }
        return vuser.state;
    },
    async (api, vuser) => {
        if (!vuser.isLoggedIn() || ownsSelectedPublication(vuser)) return false;
        const publication = asPublication(vuser.selectedPublication);
        if (!publication || !isActivePublication(publication)) return false;
        const cards = await ensureCollection(api, vuser);
        return pickTradableCards(cards, 1).length > 0;
    },
);

export const ACCEPT_PROPOSAL = new Transition(
    "ACCEPT_PROPOSAL",
    async (api, vuser, users) => {
        const proposal = pickPendingReceivedProposal(vuser);
        if (!proposal || !vuser.userId) return vuser.state;

        await api.acceptProposal(vuser, proposal.id);
        const proposerId = proposal.proposer?.id ?? "";
        syncProposalStatusBetweenUsers(users, proposal.id, vuser.userId, proposerId, "ACCEPTED");

        const publisher = findVUserByUserId(users, vuser.userId);
        const proposer = findVUserByUserId(users, proposerId);
        if (publisher) {
            await refreshUserExchanges(api, publisher);
            await refreshUserCollection(api, publisher);
        }
        if (proposer) {
            await refreshUserExchanges(api, proposer);
            await refreshUserCollection(api, proposer);
        }
        return vuser.state;
    },
    async (_api, vuser) => vuser.isLoggedIn() && pickPendingReceivedProposal(vuser) !== null,
);

export const REJECT_PROPOSAL = new Transition(
    "REJECT_PROPOSAL",
    async (api, vuser, users) => {
        const proposal = pickPendingReceivedProposal(vuser);
        if (!proposal || !vuser.userId) return vuser.state;

        await api.rejectProposal(vuser, proposal.id);
        const proposerId = proposal.proposer?.id ?? "";
        syncProposalStatusBetweenUsers(users, proposal.id, vuser.userId, proposerId, "REJECTED");
        return vuser.state;
    },
    async (_api, vuser) => vuser.isLoggedIn() && pickPendingReceivedProposal(vuser) !== null,
);

export const CANCEL_PROPOSAL = new Transition(
    "CANCEL_PROPOSAL",
    async (api, vuser, users) => {
        const proposal = pickPendingSentProposal(vuser);
        if (!proposal || !vuser.userId) return vuser.state;

        await api.cancelProposal(vuser, proposal.id);
        const publisherId = proposal.receiver?.id ?? "";
        syncProposalStatusBetweenUsers(users, proposal.id, publisherId, vuser.userId, "CANCELLED");
        return vuser.state;
    },
    async (_api, vuser) => vuser.isLoggedIn() && pickPendingSentProposal(vuser) !== null,
);

export const ACCEPT_OFFER = new Transition(
    "ACCEPT_OFFER",
    async (api, vuser, users) => {
        const target = pickPendingOfferOnOwnedAuction(vuser);
        if (!target || !vuser.userId) return vuser.state;

        await api.acceptOffer(vuser, target.auctionId, target.offerId);
        const refreshed = await api.getAuctionById(vuser, target.auctionId);
        vuser.selectedAuction = refreshed;
        upsertAuction(vuser, refreshed);

        const auction = asAuction(refreshed);
        const pendingOffer = auction?.offers?.find((o) => o.id === target.offerId);
        const bidderId = pendingOffer?.bidderUserId;
        if (bidderId) {
            const bidder = findVUserByUserId(users, bidderId);
            if (bidder) {
                await refreshUserExchanges(api, bidder);
                await refreshUserCollection(api, bidder);
                bidder.myBids = await api.getAuctionBidsByUserId(bidder, bidder.userId!);
            }
        }
        await refreshUserExchanges(api, vuser);
        await refreshUserCollection(api, vuser);
        return vuser.state;
    },
    async (_api, vuser) => vuser.isLoggedIn() && pickPendingOfferOnOwnedAuction(vuser) !== null,
);

export const REJECT_OFFER = new Transition(
    "REJECT_OFFER",
    async (api, vuser, users) => {
        const target = pickPendingOfferOnOwnedAuction(vuser);
        if (!target) return vuser.state;

        await api.rejectOffer(vuser, target.auctionId, target.offerId);
        const refreshed = await api.getAuctionById(vuser, target.auctionId);
        vuser.selectedAuction = refreshed;
        upsertAuction(vuser, refreshed);

        const auction = asAuction(refreshed);
        const offer = auction?.offers?.find((o) => o.id === target.offerId);
        if (offer?.bidderUserId) {
            const bidder = findVUserByUserId(users, offer.bidderUserId);
            if (bidder) {
                bidder.myBids = await api.getAuctionBidsByUserId(bidder, bidder.userId!);
            }
        }
        return vuser.state;
    },
    async (_api, vuser) => vuser.isLoggedIn() && pickPendingOfferOnOwnedAuction(vuser) !== null,
);

export const CANCEL_OFFER = new Transition(
    "CANCEL_OFFER",
    async (api, vuser, users) => {
        const target = pickOwnPendingBid(vuser);
        if (!target || !target.offerId) return vuser.state;

        await api.cancelOffer(vuser, target.auctionId, target.offerId);
        vuser.myBids = vuser.userId
            ? await api.getAuctionBidsByUserId(vuser, vuser.userId)
            : [];

        const auction = await api.getAuctionById(vuser, target.auctionId);
        const publisherId = asAuction(auction) ? auctionPublisherId(asAuction(auction)!) : null;
        if (publisherId) {
            const owner = findVUserByUserId(users, publisherId);
            if (owner) {
                if (owner.selectedAuctionId === target.auctionId) {
                    owner.selectedAuction = auction;
                }
                upsertAuction(owner, auction);
            }
        }
        await refreshUserCollection(api, vuser);
        return vuser.state;
    },
    async (_api, vuser) => vuser.isLoggedIn() && pickOwnPendingBid(vuser) !== null,
);

export const CANCEL_AUCTION = new Transition(
    "CANCEL_AUCTION",
    async (api, vuser) => {
        const auction = asAuction(vuser.selectedAuction);
        if (!auction || !ownsSelectedAuction(vuser)) return vuser.state;

        await api.cancelAuction(vuser, auction.id);
        await refreshUserCollection(api, vuser);
        vuser.auctions = vuser.userId
            ? await api.getAuctionsByUserId(vuser, vuser.userId)
            : [];
        vuser.selectedAuction = await api.getAuctionById(vuser, auction.id);
        return vuser.state;
    },
    async (_api, vuser) => {
        if (!vuser.isLoggedIn() || !ownsSelectedAuction(vuser)) return false;
        const auction = asAuction(vuser.selectedAuction);
        return auction !== null && isActiveAuction(auction);
    },
);

export const CANCEL_PUBLICATION = new Transition(
    "CANCEL_PUBLICATION",
    async (api, vuser) => {
        const publication = asPublication(vuser.selectedPublication);
        if (!publication || !ownsSelectedPublication(vuser)) return vuser.state;

        await api.cancelPublication(vuser, publication.id);
        await refreshUserCollection(api, vuser);
        vuser.publications = vuser.userId
            ? await api.getMyPublications(vuser, vuser.userId)
            : [];
        vuser.selectedPublication = await api.getPublicationById(vuser, publication.id);
        return vuser.state;
    },
    async (_api, vuser) => {
        if (!vuser.isLoggedIn() || !ownsSelectedPublication(vuser)) return false;
        const publication = asPublication(vuser.selectedPublication);
        return publication !== null && isActivePublication(publication);
    },
);

export const WRITE_TRANSITIONS = [
    CREATE_AUCTION,
    PUBLISH_CARD,
    PLACE_BID,
    MAKE_PROPOSAL,
    ACCEPT_PROPOSAL,
    REJECT_PROPOSAL,
    CANCEL_PROPOSAL,
    ACCEPT_OFFER,
    REJECT_OFFER,
    CANCEL_OFFER,
    CANCEL_AUCTION,
    CANCEL_PUBLICATION,
];
