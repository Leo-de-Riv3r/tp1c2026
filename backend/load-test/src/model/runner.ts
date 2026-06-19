import { IDLE_STATE, UNLOGGED_STATE } from "../data/states.js";
import { API } from "./api.js";
import { Config } from "./config.js";
import { VUser } from "./vuser.js";

export class Runner {

    api: API;
    vusers: VUser[];
    config: Config;

    constructor(api: API, vusers: VUser[], config: Config) {
        this.api = api;
        this.vusers = vusers;
        this.config = config;
    }

    async execute() {
        const startTime = Date.now();
        const endTime = startTime + this.config.duration * 1000;

        while (Date.now() < endTime) {
            const elapsedTime = Date.now() - startTime;
            const expectedUsers = Math.min(
                this.config.start_users + Math.floor((elapsedTime / (this.config.ramp_up_time * 1000)) * (this.config.max_users - this.config.start_users)),
                this.config.max_users,
            );

            if (this.activeUsers().length < expectedUsers) {
                for (let i = 0; i < expectedUsers - this.activeUsers().length; i++) {
                    const idleUsers = this.idleUsers();
                    if (idleUsers.length > 0) {
                        const userToActivate = idleUsers[Math.floor(Math.random() * idleUsers.length)];
                        userToActivate.state = UNLOGGED_STATE;
                    }
                }
            }

            if (this.activeUsers().length > expectedUsers) {
                for (let i = 0; i < this.activeUsers().length - expectedUsers; i++) {
                    const activeUsers = this.activeUsers();
                    if (activeUsers.length > 0) {
                        const userToDeactivate = activeUsers[Math.floor(Math.random() * activeUsers.length)];
                        userToDeactivate.state = IDLE_STATE;
                    }
                }
            }

            await Promise.all(
                this.vusers.map((user) => user.step(this.api, this.vusers, this.config)),
            );
        }
    }

    idleUsers(): VUser[] {
        return this.vusers.filter((vuser) => vuser.state === IDLE_STATE);
    }

    activeUsers(): VUser[] {
        return this.vusers.filter((vuser) => vuser.state !== IDLE_STATE);
    }
}
