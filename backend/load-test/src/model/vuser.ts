import {State} from "./state.js";
import {Card, CardCollection} from "./card.js";
import {API} from "./api.js";
import {Config} from "./config.js";
import {SearchAvailableResponse} from "./types.js";

export class VUser {

    state: State;
    available_cards: CardCollection[];
    missing_cards: Card[];
    think_time: number;
    email: string;
    password: string;

    token: string | null = null;
    userId: string | null = null;
    previousStateId: string | null = null;

    suggestions: unknown[] = [];
    catalog: unknown[] = [];
    auctions: unknown[] = [];
    publications: unknown[] = [];
    proposals: unknown[] = [];
    exchanges: unknown[] = [];
    notifications: unknown[] = [];
    searchResults: SearchAvailableResponse | null = null;
    searchFilters: Record<string, unknown> | null = null;
    searchPubPage = 1;
    searchAucPage = 1;

    selectedAuctionId: string | null = null;
    selectedPublicationId: string | null = null;
    selectedAuction: unknown | null = null;
    selectedPublication: unknown | null = null;
    collection: unknown[] = [];
    myBids: unknown[] = [];
    missingCardsCache: unknown[] = [];

    constructor(
        initial_state: State,
        available_cards: CardCollection[],
        missing_cards: Card[],
        think_time: number,
        email: string,
        password: string,
    ) {
        this.state = initial_state;
        this.available_cards = available_cards;
        this.missing_cards = missing_cards;
        this.email = email;
        this.password = password;
        this.think_time = think_time;
    }

    clearSession(): void {
        this.token = null;
        this.userId = null;
        this.suggestions = [];
        this.catalog = [];
        this.auctions = [];
        this.publications = [];
        this.proposals = [];
        this.exchanges = [];
        this.notifications = [];
        this.searchResults = null;
        this.searchFilters = null;
        this.selectedAuctionId = null;
        this.selectedPublicationId = null;
        this.selectedAuction = null;
        this.selectedPublication = null;
        this.collection = [];
        this.myBids = [];
        this.missingCardsCache = [];
        this.previousStateId = null;
    }

    isLoggedIn(): boolean {
        return this.token !== null && this.userId !== null;
    }

    async step(api: API, users: VUser[], config: Config) {
        const transition = await this.state.selectRandomTransition(api, this, users, config);
        if (transition) {
            this.state = await transition.execute(api, this, users);
        }
        await new Promise(resolve => setTimeout(resolve, this.think_time));
    }
}
