import {
    CreateAuctionData,
    FeedbackData,
    LoginResult,
    MakeProposalData,
    Paginated,
    PublishData,
    RegisterData,
    SearchAvailableResponse,
    SearchFilters,
} from "./types.js";
import { VUser } from "./vuser.js";

export interface API {
    login(email: string, password: string): Promise<LoginResult>;
    register(data: RegisterData): Promise<LoginResult>;
    logout(vuser: VUser): Promise<void>;

    getById(vuser: VUser, userId: string): Promise<unknown>;
    getUserCollection(vuser: VUser, userId: string): Promise<unknown[]>;
    addToUserCollection(vuser: VUser, userId: string, cardId: string): Promise<unknown>;
    getUserMissingCards(vuser: VUser, userId: string): Promise<unknown[]>;
    addMissingCard(vuser: VUser, userId: string, cardId: string): Promise<unknown>;
    removeMissingCard(vuser: VUser, userId: string, cardId: string): Promise<void>;
    getUserSuggestions(vuser: VUser, userId: string): Promise<unknown[]>;

    getCatalog(vuser: VUser): Promise<unknown[]>;
    getCatalogCardById(vuser: VUser, id: string): Promise<unknown>;
    searchAvailable(vuser: VUser, filters: SearchFilters): Promise<SearchAvailableResponse>;

    getMyPublications(vuser: VUser, userId: string, status?: string): Promise<unknown[]>;
    getPublicationById(vuser: VUser, id: string): Promise<unknown>;
    publishFigurita(vuser: VUser, data: PublishData): Promise<unknown>;
    cancelPublication(vuser: VUser, id: string): Promise<void>;

    getProposals(vuser: VUser, publisherId: string, bidderId: string, status?: string): Promise<unknown[]>;
    getProposalsByPublicationId(vuser: VUser, publicationId: string): Promise<unknown[]>;
    makeProposal(vuser: VUser, data: MakeProposalData): Promise<unknown>;
    acceptProposal(vuser: VUser, proposalId: string): Promise<unknown>;
    rejectProposal(vuser: VUser, proposalId: string): Promise<void>;
    cancelProposal(vuser: VUser, proposalId: string): Promise<void>;

    getActiveAuctions(vuser: VUser): Promise<unknown[]>;
    getAuctionsByUserId(vuser: VUser, userId: string): Promise<unknown[]>;
    getAuctionById(vuser: VUser, id: string): Promise<unknown>;
    createAuction(vuser: VUser, data: CreateAuctionData): Promise<unknown>;
    placeBid(vuser: VUser, auctionId: string, cardIds: Array<{ cardId: string; amount: number }>): Promise<unknown>;
    getAuctionBidsByUserId(vuser: VUser, userId: string): Promise<unknown[]>;
    acceptOffer(vuser: VUser, auctionId: string, offerId: string): Promise<void>;
    rejectOffer(vuser: VUser, auctionId: string, offerId: string): Promise<void>;
    cancelAuction(vuser: VUser, auctionId: string): Promise<void>;
    cancelOffer(vuser: VUser, auctionId: string, offerId: string): Promise<void>;

    getExchangesByUserId(vuser: VUser, userId: string, page?: number, perPage?: number): Promise<unknown[]>;
    getExchangeById(vuser: VUser, id: string): Promise<unknown>;
    submitFeedback(vuser: VUser, exchangeId: string, data: FeedbackData): Promise<void>;

    getNotifications(vuser: VUser, userId: string, page: number, perPage: number, status: "UNREAD" | "READ"): Promise<Paginated<unknown>>;
    markAllAsRead(vuser: VUser, userId: string): Promise<void>;
    markAsRead(vuser: VUser, userId: string, notificationId: string): Promise<void>;
}
