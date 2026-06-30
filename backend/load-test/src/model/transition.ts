import { API } from "./api.js";
import { State } from "./state.js";
import { VUser } from "./vuser.js";

type TransitionFunc = (api : API, vuser : VUser, users : VUser[]) => Promise<State>;
type ValidationFunc = (api : API, vuser : VUser, users : VUser[]) => Promise<boolean>;

export class Transition {

    id: string;
    transitionFunc: TransitionFunc;
    validationFunc: ValidationFunc;

    constructor(id: string, transitionFunc: TransitionFunc, validationFunc: ValidationFunc) {
        this.id = id;
        this.transitionFunc = transitionFunc;
        this.validationFunc = validationFunc;
    }

    async execute(api : API, vuser : VUser, users : VUser[]) {
        return this.transitionFunc(api, vuser, users);
    }

    async isValidTransition(api : API, vuser : VUser, users : VUser[]) {
        return this.validationFunc(api, vuser, users);
    }


}