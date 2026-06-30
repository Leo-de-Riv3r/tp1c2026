import { buildEndpoints, Endpoints } from "./endpoints.js";
import { MetricsRegistry } from "./metrics.js";
import { API } from "../model/api.js";
import { Config } from "../model/config.js";
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
} from "../model/types.js";
import { VUser } from "../model/vuser.js";

type HttpMethod = "GET" | "POST" | "PUT" | "DELETE" | "PATCH";

export class LoadRunnerApi implements API {
    private endpoints: Endpoints;
    readonly metrics: MetricsRegistry;

    constructor(private config: Config, metrics?: MetricsRegistry) {
        this.endpoints = buildEndpoints(config.api_base_url);
        this.metrics = metrics ?? new MetricsRegistry();
    }

    private async request<T>(
        method: HttpMethod,
        url: string,
        endpointLabel: string,
        vuser?: VUser,
        body?: unknown,
        query?: Record<string, string | number | undefined>,
    ): Promise<T> {
        const qs = query
            ? "?" + Object.entries(query)
                .filter(([, v]) => v !== undefined && v !== "")
                .map(([k, v]) => `${encodeURIComponent(k)}=${encodeURIComponent(String(v))}`)
                .join("&")
            : "";
        const fullUrl = url + qs;
        const headers: Record<string, string> = { "Content-Type": "application/json" };
        if (vuser?.token) {
            headers["Authorization"] = `Bearer ${vuser.token}`;
        }
        const start = Date.now();
        let success = false;
        try {
            const response = await fetch(fullUrl, {
                method,
                headers,
                body: body !== undefined ? JSON.stringify(body) : undefined,
            });
            success = response.ok;
            if (!response.ok) {
                throw new Error(`${method} ${endpointLabel} failed: ${response.status}`);
            }
            if (response.status === 204) {
                return undefined as T;
            }
            const text = await response.text();
            if (!text) return undefined as T;
            return JSON.parse(text) as T;
        } finally {
            this.metrics.recordRequest(`${method} ${endpointLabel}`, Date.now() - start, success);
        }
    }

    private paginatedData<T>(result: Paginated<T> | T[]): T[] {
        if (Array.isArray(result)) return result;
        return result?.data ?? [];
    }

    async login(email: string, password: string): Promise<LoginResult> {
        return this.request<LoginResult>("POST", this.endpoints.auth.login, "/auth/login", undefined, { email, password });
    }

    async register(data: RegisterData): Promise<LoginResult> {
        return this.request<LoginResult>("POST", this.endpoints.auth.register, "/auth/register", undefined, data);
    }

    async logout(vuser: VUser): Promise<void> {
        await this.request<void>("POST", this.endpoints.auth.logout, "/auth/logout", vuser, null);
    }

    async getById(vuser: VUser, userId: string): Promise<unknown> {
        return this.request("GET", this.endpoints.users.byId(userId), `/users/{id}`, vuser);
    }

    async getUserCollection(vuser: VUser, userId: string): Promise<unknown[]> {
        return this.request("GET", this.endpoints.users.collection(userId), `/users/{id}/collection`, vuser);
    }

    async addToUserCollection(vuser: VUser, userId: string, cardId: string): Promise<unknown> {
        return this.request("POST", this.endpoints.users.collection(userId), `/users/{id}/collection`, vuser, { cardId });
    }

    async getUserMissingCards(vuser: VUser, userId: string): Promise<unknown[]> {
        return this.request("GET", this.endpoints.users.missingCards(userId), `/users/{id}/missing-cards`, vuser);
    }

    async addMissingCard(vuser: VUser, userId: string, cardId: string): Promise<unknown> {
        return this.request("POST", this.endpoints.users.missingCards(userId), `/users/{id}/missing-cards`, vuser, { cardId });
    }

    async removeMissingCard(vuser: VUser, userId: string, cardId: string): Promise<void> {
        await this.request("DELETE", this.endpoints.users.missingCard(userId, cardId), `/users/{id}/missing-cards/{cardId}`, vuser);
    }

    async getUserSuggestions(vuser: VUser, userId: string): Promise<unknown[]> {
        return this.request("GET", this.endpoints.users.suggestions(userId), `/users/{id}/suggestions`, vuser);
    }

    async getCatalog(vuser: VUser): Promise<unknown[]> {
        return this.request("GET", this.endpoints.cards.catalog, "/cards/catalog", vuser);
    }

    async getCatalogCardById(vuser: VUser, id: string): Promise<unknown> {
        return this.request("GET", this.endpoints.cards.catalogById(id), "/cards/catalog/{id}", vuser);
    }

    async searchAvailable(vuser: VUser, filters: SearchFilters): Promise<SearchAvailableResponse> {
        const query: Record<string, string | number | undefined> = {};
        if (filters.number != null) query.number = filters.number;
        if (filters.description) query.description = filters.description;
        if (filters.country) query.country = filters.country;
        if (filters.team) query.team = filters.team;
        if (filters.category) query.category = filters.category.toUpperCase();
        if (filters.cardType) query.cardType = filters.cardType.toUpperCase();
        if (filters.pubPage != null) query.pubPage = filters.pubPage;
        if (filters.pubPerPage != null) query.pubPerPage = filters.pubPerPage;
        if (filters.aucPage != null) query.aucPage = filters.aucPage;
        if (filters.aucPerPage != null) query.aucPerPage = filters.aucPerPage;
        return this.request("GET", this.endpoints.cards.search, "/cards/search", vuser, undefined, query);
    }

    async getMyPublications(vuser: VUser, userId: string, status?: string): Promise<unknown[]> {
        const result = await this.request<Paginated<unknown>>("GET", this.endpoints.publications.base, "/publications", vuser, undefined, { userId, status });
        return this.paginatedData(result);
    }

    async getPublicationById(vuser: VUser, id: string): Promise<unknown> {
        return this.request("GET", this.endpoints.publications.byId(id), "/publications/{id}", vuser);
    }

    async publishFigurita(vuser: VUser, data: PublishData): Promise<unknown> {
        const result = await this.request<{ data: unknown }>("POST", this.endpoints.publications.base, "/publications", vuser, data);
        return result?.data ?? result;
    }

    async cancelPublication(vuser: VUser, id: string): Promise<void> {
        await this.request("DELETE", this.endpoints.publications.byId(id), "/publications/{id}", vuser);
    }

    async getProposals(vuser: VUser, publisherId: string, bidderId: string, status?: string): Promise<unknown[]> {
        const query: Record<string, string | number | undefined> = {};
        if (publisherId) {
            query.userId = publisherId;
            query.role = "publisher";
        } else if (bidderId) {
            query.userId = bidderId;
            query.role = "proposer";
        }
        if (status) query.status = status;
        return this.request("GET", this.endpoints.proposals.base, "/proposals", vuser, undefined, query);
    }

    async getProposalsByPublicationId(vuser: VUser, publicationId: string): Promise<unknown[]> {
        return this.request("GET", this.endpoints.proposals.base, "/proposals", vuser, undefined, { publicationId });
    }

    async makeProposal(vuser: VUser, data: MakeProposalData): Promise<unknown> {
        const result = await this.request<{ data: unknown }>("POST", this.endpoints.proposals.base, "/proposals", vuser, data);
        return result?.data ?? result;
    }

    async acceptProposal(vuser: VUser, proposalId: string): Promise<unknown> {
        const result = await this.request<{ data: unknown }>("PUT", this.endpoints.proposals.accept(proposalId), "/proposals/{id}/accept", vuser);
        return result?.data ?? result;
    }

    async rejectProposal(vuser: VUser, proposalId: string): Promise<void> {
        await this.request("PUT", this.endpoints.proposals.reject(proposalId), "/proposals/{id}/reject", vuser);
    }

    async cancelProposal(vuser: VUser, proposalId: string): Promise<void> {
        await this.request("PUT", this.endpoints.proposals.cancel(proposalId), "/proposals/{id}/cancel", vuser);
    }

    async getActiveAuctions(vuser: VUser): Promise<unknown[]> {
        const result = await this.request<Paginated<unknown>>("GET", this.endpoints.auctions.base, "/auctions", vuser);
        return this.paginatedData(result);
    }

    async getAuctionsByUserId(vuser: VUser, userId: string): Promise<unknown[]> {
        const result = await this.request<Paginated<unknown>>("GET", this.endpoints.auctions.base, "/auctions", vuser, undefined, { userId });
        return this.paginatedData(result);
    }

    async getAuctionById(vuser: VUser, id: string): Promise<unknown> {
        return this.request("GET", this.endpoints.auctions.byId(id), "/auctions/{id}", vuser);
    }

    async createAuction(vuser: VUser, data: CreateAuctionData): Promise<unknown> {
        const result = await this.request<{ data: unknown }>("POST", this.endpoints.auctions.base, "/auctions", vuser, data);
        return result?.data ?? result;
    }

    async placeBid(vuser: VUser, auctionId: string, cardIds: Array<{ cardId: string; amount: number }>): Promise<unknown> {
        const result = await this.request<{ data: unknown }>("POST", this.endpoints.auctions.placeOffer(auctionId), "/auctions/{id}/offers", vuser, { items: cardIds });
        return result?.data ?? result;
    }

    async getAuctionBidsByUserId(vuser: VUser, userId: string): Promise<unknown[]> {
        return this.request("GET", this.endpoints.auctions.offers, "/auctions/offers", vuser, undefined, { userId });
    }

    async acceptOffer(vuser: VUser, auctionId: string, offerId: string): Promise<void> {
        await this.request("PUT", this.endpoints.auctions.acceptOffer(auctionId, offerId), "/auctions/{id}/offers/{offerId}/accept", vuser);
    }

    async rejectOffer(vuser: VUser, auctionId: string, offerId: string): Promise<void> {
        await this.request("PUT", this.endpoints.auctions.rejectOffer(auctionId, offerId), "/auctions/{id}/offers/{offerId}/reject", vuser);
    }

    async cancelAuction(vuser: VUser, auctionId: string): Promise<void> {
        await this.request("DELETE", this.endpoints.auctions.byId(auctionId), "/auctions/{id}", vuser);
    }

    async cancelOffer(vuser: VUser, auctionId: string, offerId: string): Promise<void> {
        await this.request("DELETE", this.endpoints.auctions.cancelOffer(auctionId, offerId), "/auctions/{id}/offers/{offerId}", vuser);
    }

    async getExchangesByUserId(vuser: VUser, userId: string, page = 1, perPage = 20): Promise<unknown[]> {
        const result = await this.request<Paginated<unknown>>("GET", this.endpoints.exchanges.base, "/exchanges", vuser, undefined, { userId, page, per_page: perPage });
        return this.paginatedData(result);
    }

    async getExchangeById(vuser: VUser, id: string): Promise<unknown> {
        return this.request("GET", this.endpoints.exchanges.byId(id), "/exchanges/{id}", vuser);
    }

    async submitFeedback(vuser: VUser, exchangeId: string, data: FeedbackData): Promise<void> {
        await this.request("POST", this.endpoints.exchanges.feedback(exchangeId), "/exchanges/{id}/feedback", vuser, data);
    }

    async getNotifications(vuser: VUser, userId: string, page: number, perPage: number, status: "UNREAD" | "READ"): Promise<Paginated<unknown>> {
        return this.request("GET", this.endpoints.users.notifications(userId), `/users/{id}/notifications`, vuser, undefined, { page, per_page: perPage, status });
    }

    async markAllAsRead(vuser: VUser, userId: string): Promise<void> {
        await this.request("PUT", this.endpoints.users.notificationsRead(userId), `/users/{id}/notifications/read`, vuser);
    }

    async markAsRead(vuser: VUser, userId: string, notificationId: string): Promise<void> {
        await this.request("PUT", this.endpoints.users.notificationRead(userId, notificationId), `/users/{id}/notifications/{notificationId}/read`, vuser);
    }
}
