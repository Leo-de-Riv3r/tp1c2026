export interface Paginated<T> {
    data: T[];
    currentPage: number;
    totalPages: number;
}

export interface LoginResult {
    token: string;
    user: { id: string; name?: string; email?: string };
}

export interface RegisterData {
    name: string;
    email: string;
    password: string;
    avatarId: string;
}

export interface SearchFilters {
    number?: number;
    description?: string;
    country?: string;
    team?: string;
    category?: string;
    cardType?: string;
    pubPage?: number;
    pubPerPage?: number;
    aucPage?: number;
    aucPerPage?: number;
}

export interface SearchAvailableResponse {
    publications: Paginated<{ id: string }>;
    auctions: Paginated<{ id: string }>;
}

export interface CreateAuctionData {
    cardId: string;
    auctionDurationHours: number;
    conditions?: Array<{ filterName: string; quantity?: number; value?: string }>;
}

export interface PublishData {
    cardId: string;
    quantity: number;
}

export interface MakeProposalData {
    publicationId: string;
    cardIds: string[];
    requestedCount: number;
}

export interface FeedbackData {
    score: number;
    comment?: string;
}
