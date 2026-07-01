import { State } from "../model/state.js";
import { StateId } from "./state_ids.js";

export const STATE_REGISTRY: Record<string, State> = {};

export function registerState(id: StateId, state: State): void {
    STATE_REGISTRY[id] = state;
}

export function getState(id: StateId): State {
    const state = STATE_REGISTRY[id];
    if (!state) throw new Error(`State not registered: ${id}`);
    return state;
}
