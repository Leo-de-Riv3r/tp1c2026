import { API } from "../model/api.js";
import { Transition } from "../model/transition.js";
import { VUser } from "../model/vuser.js";
import { getState } from "./state_registry.js";
import { StateIds } from "./state_ids.js";

function pickRandom<T>(items: T[]): T | null {
    if (items.length === 0) return null;
    return items[Math.floor(Math.random() * items.length)];
}

function pickAuctionId(vuser: VUser): string | null {
    const fromSuggestions = vuser.suggestions
        .filter((s: unknown) => (s as { sourceType?: string }).sourceType === "AUCTION")
        .map((s: unknown) => (s as { sourceId: string }).sourceId);
    if (fromSuggestions.length > 0) return pickRandom(fromSuggestions);
    const fromSearch = vuser.searchResults?.auctions?.data?.map((a: { id: string }) => a.id) ?? [];
    if (fromSearch.length > 0) return pickRandom(fromSearch);
    const fromAuctions = vuser.auctions.map((a: unknown) => (a as { id: string }).id);
    if (fromAuctions.length > 0) return pickRandom(fromAuctions);
    return null;
}

function pickPublicationId(vuser: VUser): string | null {
    const fromSuggestions = vuser.suggestions
        .filter((s: unknown) => (s as { sourceType?: string }).sourceType === "PUBLICATION")
        .map((s: unknown) => (s as { sourceId: string }).sourceId);
    if (fromSuggestions.length > 0) return pickRandom(fromSuggestions);
    const fromSearch = vuser.searchResults?.publications?.data?.map((p: { id: string }) => p.id) ?? [];
    if (fromSearch.length > 0) return pickRandom(fromSearch);
    const fromPubs = vuser.publications.map((p: unknown) => (p as { id: string }).id);
    if (fromPubs.length > 0) return pickRandom(fromPubs);
    return null;
}

async function loadHomeData(api: API, vuser: VUser): Promise<void> {
    if (!vuser.userId) return;
    vuser.suggestions = await api.getUserSuggestions(vuser, vuser.userId);
    vuser.catalog = await api.getCatalog(vuser);
}

const loggedIn = async (_api: API, vuser: VUser) => vuser.isLoggedIn();
const notLoggedIn = async (_api: API, vuser: VUser) => !vuser.isLoggedIn();
const hasAuctionTarget = async (_api: API, vuser: VUser) => pickAuctionId(vuser) !== null;
const hasPublicationTarget = async (_api: API, vuser: VUser) => pickPublicationId(vuser) !== null;
const canGoBack = async (_api: API, vuser: VUser) => vuser.previousStateId !== null;

export const LOG_IN = new Transition(
    "LOG_IN",
    async (api, vuser) => {
        const result = await api.login(vuser.email, vuser.password);
        vuser.token = result.token;
        vuser.userId = result.user.id;
        await loadHomeData(api, vuser);
        return getState(StateIds.HOME);
    },
    notLoggedIn,
);

export const LOG_OUT = new Transition(
    "LOG_OUT",
    async (api, vuser) => {
        if (vuser.token) {
            try { await api.logout(vuser); } catch { /* session cleanup is best-effort */ }
        }
        vuser.clearSession();
        return getState(StateIds.UNLOGGED);
    },
    loggedIn,
);

export const GO_HOME = new Transition(
    "GO_HOME",
    async (api, vuser) => {
        await loadHomeData(api, vuser);
        return getState(StateIds.HOME);
    },
    loggedIn,
);

export const GO_CATALOG = new Transition(
    "GO_CATALOG",
    async (api, vuser) => {
        vuser.catalog = await api.getCatalog(vuser);
        return getState(StateIds.CATALOG);
    },
    loggedIn,
);

export const GO_SEARCH = new Transition(
    "GO_SEARCH",
    async () => getState(StateIds.SEARCH),
    loggedIn,
);

export const RUN_SEARCH = new Transition(
    "RUN_SEARCH",
    async (api, vuser) => {
        vuser.searchFilters = {};
        vuser.searchPubPage = 1;
        vuser.searchAucPage = 1;
        vuser.searchResults = await api.searchAvailable(vuser, { pubPage: 1, pubPerPage: 10, aucPage: 1, aucPerPage: 10 });
        return getState(StateIds.SEARCH_RESULTS);
    },
    loggedIn,
);

export const SEARCH_NEXT_PAGE = new Transition(
    "SEARCH_NEXT_PAGE",
    async (api, vuser) => {
        const pubPages = vuser.searchResults?.publications?.totalPages ?? 1;
        const aucPages = vuser.searchResults?.auctions?.totalPages ?? 1;
        if (vuser.searchPubPage < pubPages) vuser.searchPubPage++;
        else if (vuser.searchAucPage < aucPages) vuser.searchAucPage++;
        vuser.searchResults = await api.searchAvailable(vuser, {
            pubPage: vuser.searchPubPage,
            pubPerPage: 10,
            aucPage: vuser.searchAucPage,
            aucPerPage: 10,
        });
        return getState(StateIds.SEARCH_RESULTS);
    },
    async (_api, vuser) => {
        if (!vuser.isLoggedIn() || !vuser.searchResults) return false;
        const pubPages = vuser.searchResults.publications?.totalPages ?? 1;
        const aucPages = vuser.searchResults.auctions?.totalPages ?? 1;
        return vuser.searchPubPage < pubPages || vuser.searchAucPage < aucPages;
    },
);

export const GO_AUCTIONS_ACTIVE = new Transition(
    "GO_AUCTIONS_ACTIVE",
    async (api, vuser) => {
        vuser.auctions = await api.getActiveAuctions(vuser);
        return getState(StateIds.AUCTIONS_ACTIVE);
    },
    loggedIn,
);

export const GO_AUCTIONS_MY = new Transition(
    "GO_AUCTIONS_MY",
    async (api, vuser) => {
        if (!vuser.userId) return getState(StateIds.AUCTIONS_MY);
        vuser.auctions = await api.getAuctionsByUserId(vuser, vuser.userId);
        return getState(StateIds.AUCTIONS_MY);
    },
    loggedIn,
);

export const GO_AUCTIONS_MY_BIDS = new Transition(
    "GO_AUCTIONS_MY_BIDS",
    async (api, vuser) => {
        if (!vuser.userId) return getState(StateIds.AUCTIONS_MY_BIDS);
        vuser.myBids = await api.getAuctionBidsByUserId(vuser, vuser.userId);
        return getState(StateIds.AUCTIONS_MY_BIDS);
    },
    loggedIn,
);

export const GO_AUCTION_CREATE = new Transition(
    "GO_AUCTION_CREATE",
    async (api, vuser) => {
        vuser.catalog = await api.getCatalog(vuser);
        return getState(StateIds.AUCTION_CREATE);
    },
    loggedIn,
);

export const OPEN_AUCTION_DETAIL = new Transition(
    "OPEN_AUCTION_DETAIL",
    async (api, vuser) => {
        const id = pickAuctionId(vuser);
        if (!id) return vuser.state;
        vuser.previousStateId = vuser.state.id;
        vuser.selectedAuctionId = id;
        vuser.selectedAuction = await api.getAuctionById(vuser, id);
        return getState(StateIds.AUCTION_DETAIL);
    },
    hasAuctionTarget,
);

export const OPEN_PUBLICATION_DETAIL = new Transition(
    "OPEN_PUBLICATION_DETAIL",
    async (api, vuser) => {
        const id = pickPublicationId(vuser);
        if (!id) return vuser.state;
        vuser.previousStateId = vuser.state.id;
        vuser.selectedPublicationId = id;
        vuser.selectedPublication = await api.getPublicationById(vuser, id);
        vuser.proposals = await api.getProposalsByPublicationId(vuser, id);
        return getState(StateIds.PUBLICATION_DETAIL);
    },
    hasPublicationTarget,
);

export const GO_PROFILE_COLLECTION = new Transition(
    "GO_PROFILE_COLLECTION",
    async (api, vuser) => {
        if (!vuser.userId) return getState(StateIds.PROFILE_COLLECTION);
        vuser.collection = await api.getUserCollection(vuser, vuser.userId);
        return getState(StateIds.PROFILE_COLLECTION);
    },
    loggedIn,
);

export const GO_PROFILE_MISSING = new Transition(
    "GO_PROFILE_MISSING",
    async (api, vuser) => {
        if (!vuser.userId) return getState(StateIds.PROFILE_MISSING);
        vuser.missingCardsCache = await api.getUserMissingCards(vuser, vuser.userId);
        return getState(StateIds.PROFILE_MISSING);
    },
    loggedIn,
);

export const GO_PROFILE_PUBLICATIONS = new Transition(
    "GO_PROFILE_PUBLICATIONS",
    async (api, vuser) => {
        if (!vuser.userId) return getState(StateIds.PROFILE_PUBLICATIONS);
        vuser.publications = await api.getMyPublications(vuser, vuser.userId);
        return getState(StateIds.PROFILE_PUBLICATIONS);
    },
    loggedIn,
);

export const GO_PROFILE_PROPOSALS = new Transition(
    "GO_PROFILE_PROPOSALS",
    async (api, vuser) => {
        if (!vuser.userId) return getState(StateIds.PROFILE_PROPOSALS);
        vuser.proposals = await api.getProposals(vuser, vuser.userId, "");
        return getState(StateIds.PROFILE_PROPOSALS);
    },
    loggedIn,
);

export const GO_PROFILE_AUCTIONS = new Transition(
    "GO_PROFILE_AUCTIONS",
    async (api, vuser) => {
        if (!vuser.userId) return getState(StateIds.PROFILE_AUCTIONS);
        vuser.auctions = await api.getAuctionsByUserId(vuser, vuser.userId);
        vuser.myBids = await api.getAuctionBidsByUserId(vuser, vuser.userId);
        return getState(StateIds.PROFILE_AUCTIONS);
    },
    loggedIn,
);

export const GO_PROFILE_EXCHANGES = new Transition(
    "GO_PROFILE_EXCHANGES",
    async (api, vuser) => {
        if (!vuser.userId) return getState(StateIds.PROFILE_EXCHANGES);
        vuser.exchanges = await api.getExchangesByUserId(vuser, vuser.userId);
        return getState(StateIds.PROFILE_EXCHANGES);
    },
    loggedIn,
);

export const GO_PROPOSALS_RECEIVED = new Transition(
    "GO_PROPOSALS_RECEIVED",
    async (api, vuser) => {
        if (!vuser.userId) return getState(StateIds.PROPOSALS_RECEIVED);
        vuser.proposals = await api.getProposals(vuser, vuser.userId, "");
        return getState(StateIds.PROPOSALS_RECEIVED);
    },
    loggedIn,
);

export const GO_PROPOSALS_SENT = new Transition(
    "GO_PROPOSALS_SENT",
    async (api, vuser) => {
        if (!vuser.userId) return getState(StateIds.PROPOSALS_SENT);
        vuser.proposals = await api.getProposals(vuser, "", vuser.userId);
        return getState(StateIds.PROPOSALS_SENT);
    },
    loggedIn,
);

export const GO_EXCHANGES = new Transition(
    "GO_EXCHANGES",
    async (api, vuser) => {
        if (!vuser.userId) return getState(StateIds.EXCHANGES);
        vuser.exchanges = await api.getExchangesByUserId(vuser, vuser.userId);
        return getState(StateIds.EXCHANGES);
    },
    loggedIn,
);

export const GO_NOTIFICATIONS_UNREAD = new Transition(
    "GO_NOTIFICATIONS_UNREAD",
    async (api, vuser) => {
        if (!vuser.userId) return getState(StateIds.NOTIFICATIONS_UNREAD);
        const result = await api.getNotifications(vuser, vuser.userId, 1, 20, "UNREAD");
        vuser.notifications = result.data;
        return getState(StateIds.NOTIFICATIONS_UNREAD);
    },
    loggedIn,
);

export const GO_NOTIFICATIONS_READ = new Transition(
    "GO_NOTIFICATIONS_READ",
    async (api, vuser) => {
        if (!vuser.userId) return getState(StateIds.NOTIFICATIONS_READ);
        const result = await api.getNotifications(vuser, vuser.userId, 1, 20, "READ");
        vuser.notifications = result.data;
        return getState(StateIds.NOTIFICATIONS_READ);
    },
    loggedIn,
);

async function refetchForState(api: API, vuser: VUser, stateId: string): Promise<void> {
    if (!vuser.userId) return;
    switch (stateId) {
        case StateIds.HOME:
            await loadHomeData(api, vuser);
            break;
        case StateIds.CATALOG:
            vuser.catalog = await api.getCatalog(vuser);
            break;
        case StateIds.SEARCH_RESULTS:
            vuser.searchResults = await api.searchAvailable(vuser, {
                pubPage: vuser.searchPubPage,
                pubPerPage: 10,
                aucPage: vuser.searchAucPage,
                aucPerPage: 10,
            });
            break;
        case StateIds.AUCTIONS_ACTIVE:
            vuser.auctions = await api.getActiveAuctions(vuser);
            break;
        case StateIds.AUCTIONS_MY:
            vuser.auctions = await api.getAuctionsByUserId(vuser, vuser.userId);
            break;
        case StateIds.AUCTIONS_MY_BIDS:
            vuser.myBids = await api.getAuctionBidsByUserId(vuser, vuser.userId);
            break;
        case StateIds.PROPOSALS_RECEIVED:
            vuser.proposals = await api.getProposals(vuser, vuser.userId, "");
            break;
        case StateIds.PROPOSALS_SENT:
            vuser.proposals = await api.getProposals(vuser, "", vuser.userId);
            break;
        case StateIds.EXCHANGES:
            vuser.exchanges = await api.getExchangesByUserId(vuser, vuser.userId);
            break;
        default:
            await loadHomeData(api, vuser);
    }
}

export const GO_BACK = new Transition(
    "GO_BACK",
    async (api, vuser) => {
        const targetId = vuser.previousStateId ?? StateIds.HOME;
        await refetchForState(api, vuser, targetId);
        vuser.previousStateId = null;
        vuser.selectedAuctionId = null;
        vuser.selectedPublicationId = null;
        return getState(targetId as typeof StateIds[keyof typeof StateIds]);
    },
    canGoBack,
);

export const NAVBAR_TRANSITIONS: Transition[] = [
    GO_HOME,
    GO_CATALOG,
    GO_SEARCH,
    GO_AUCTIONS_ACTIVE,
    GO_EXCHANGES,
    GO_PROPOSALS_RECEIVED,
    GO_NOTIFICATIONS_UNREAD,
    GO_PROFILE_COLLECTION,
    LOG_OUT,
];

export function withNavbar(...extra: Transition[]): Transition[] {
    return [...NAVBAR_TRANSITIONS, ...extra];
}

export const ALL_TRANSITIONS: Transition[] = [
    LOG_IN,
    LOG_OUT,
    GO_HOME,
    GO_CATALOG,
    GO_SEARCH,
    RUN_SEARCH,
    SEARCH_NEXT_PAGE,
    GO_AUCTIONS_ACTIVE,
    GO_AUCTIONS_MY,
    GO_AUCTIONS_MY_BIDS,
    GO_AUCTION_CREATE,
    OPEN_AUCTION_DETAIL,
    OPEN_PUBLICATION_DETAIL,
    GO_PROFILE_COLLECTION,
    GO_PROFILE_MISSING,
    GO_PROFILE_PUBLICATIONS,
    GO_PROFILE_PROPOSALS,
    GO_PROFILE_AUCTIONS,
    GO_PROFILE_EXCHANGES,
    GO_PROPOSALS_RECEIVED,
    GO_PROPOSALS_SENT,
    GO_EXCHANGES,
    GO_NOTIFICATIONS_UNREAD,
    GO_NOTIFICATIONS_READ,
    GO_BACK,
];
