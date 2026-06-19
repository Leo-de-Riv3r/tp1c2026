export function buildEndpoints(baseUrl: string) {
    const api = `${baseUrl}/api`;
    return {
        auth: {
            login: `${api}/auth/login`,
            register: `${api}/auth/register`,
            logout: `${api}/auth/logout`,
        },
        users: {
            byId: (id: string) => `${api}/users/${id}`,
            collection: (id: string) => `${api}/users/${id}/collection`,
            missingCards: (id: string) => `${api}/users/${id}/missing-cards`,
            missingCard: (id: string, cardId: string) => `${api}/users/${id}/missing-cards/${cardId}`,
            suggestions: (id: string) => `${api}/users/${id}/suggestions`,
            notifications: (id: string) => `${api}/users/${id}/notifications`,
            notificationsRead: (id: string) => `${api}/users/${id}/notifications/read`,
            notificationRead: (id: string, notificationId: string) => `${api}/users/${id}/notifications/${notificationId}/read`,
        },
        cards: {
            catalog: `${api}/cards/catalog`,
            catalogById: (id: string) => `${api}/cards/catalog/${id}`,
            search: `${api}/cards/search`,
        },
        publications: {
            base: `${api}/publications`,
            byId: (id: string) => `${api}/publications/${id}`,
        },
        proposals: {
            base: `${api}/proposals`,
            byId: (id: string) => `${api}/proposals/${id}`,
            accept: (id: string) => `${api}/proposals/${id}/accept`,
            reject: (id: string) => `${api}/proposals/${id}/reject`,
            cancel: (id: string) => `${api}/proposals/${id}/cancel`,
        },
        auctions: {
            base: `${api}/auctions`,
            byId: (id: string) => `${api}/auctions/${id}`,
            offers: `${api}/auctions/offers`,
            placeOffer: (auctionId: string) => `${api}/auctions/${auctionId}/offers`,
            acceptOffer: (auctionId: string, offerId: string) => `${api}/auctions/${auctionId}/offers/${offerId}/accept`,
            rejectOffer: (auctionId: string, offerId: string) => `${api}/auctions/${auctionId}/offers/${offerId}/reject`,
            cancelOffer: (auctionId: string, offerId: string) => `${api}/auctions/${auctionId}/offers/${offerId}`,
        },
        exchanges: {
            base: `${api}/exchanges`,
            byId: (id: string) => `${api}/exchanges/${id}`,
            feedback: (id: string) => `${api}/exchanges/${id}/feedback`,
        },
    };
}

export type Endpoints = ReturnType<typeof buildEndpoints>;
