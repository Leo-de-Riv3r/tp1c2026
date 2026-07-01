import { API } from "./api.js";
import { Config } from "./config.js";
import { Transition } from "./transition.js";
import { VUser } from "./vuser.js";


export class State {

    id: string;
    transitions: Transition[];

    constructor(id: string, transitions: Transition[]) {
        this.id = id;
        this.transitions = transitions;
    }

    async selectRandomTransition(api: API, vuser: VUser, vusers: VUser[], config: Config): Promise<Transition | null> {
        const validationResults = await Promise.all(
            this.transitions.map(async (transition) => ({
                transition,
                valid: await transition.isValidTransition(api, vuser, vusers),
            })),
        );
        const allowedTransitions = validationResults
            .filter((r) => r.valid)
            .map((r) => r.transition);
        if (allowedTransitions.length === 0) return null;

        const totalWeight = allowedTransitions.reduce(
            (acc, transition) => acc + (config.transition_weights[transition.id] ?? 1),
            0,
        );
        if (totalWeight <= 0) return null;

        const randomWeight = Math.random() * totalWeight;
        let cumulativeWeight = 0;
        for (const transition of allowedTransitions) {
            cumulativeWeight += config.transition_weights[transition.id] ?? 1;
            if (randomWeight <= cumulativeWeight) {
                return transition;
            }
        }
        return allowedTransitions[allowedTransitions.length - 1] ?? null;
    }
}
