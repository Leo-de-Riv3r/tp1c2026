import fs from "fs";
import { IDLE_STATE } from "../data/states.js";
import { Card, CardCollection } from "../model/card.js";
import { VUser } from "../model/vuser.js";

export function createUsers(users_json_path: string): VUser[] {
    const users_json = fs.readFileSync(users_json_path, "utf-8");
    const users_data = JSON.parse(users_json);
    const users: VUser[] = [];
    for (const user_data of users_data) {
        const available_cards = (user_data.available_cards ?? []).map(
            (ac: { cardId: string; quantity: number }) => new CardCollection(ac.cardId, ac.quantity),
        );
        const missing_cards = (user_data.missing_cards ?? []).map(
            (mc: { cardId: string }) => new Card(mc.cardId),
        );
        const user = new VUser(
            IDLE_STATE,
            available_cards,
            missing_cards,
            user_data.think_time ?? 1000,
            user_data.email,
            user_data.password,
        );
        users.push(user);
    }
    return users;
}
