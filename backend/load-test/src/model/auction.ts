import { Card } from "./card.js";
import { Bid } from "./bid.js";

export class Auction {

    id: string
    card: Card
    bids: Bid[]

    constructor(id: string, card: Card, bids: Bid[]) {
        this.id = id;
        this.card = card;
        this.bids = bids;
    }

}
