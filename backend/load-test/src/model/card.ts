export class Card {
    id: string;

    constructor(id: string) {
        this.id = id;
    }

}

export class CardCollection {

    id: string;
    amount: number;

    constructor(id: string, amount: number) {
        this.id = id;
        this.amount = amount;
    }

}