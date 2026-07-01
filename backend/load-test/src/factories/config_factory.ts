import fs from "fs";
import { Config } from "../model/config.js";

export function createConfig(config_json_path: string): Config {
    const config_json = fs.readFileSync(config_json_path, "utf-8");
    const data = JSON.parse(config_json);
    const apiBaseUrl = process.env.API_BASE_URL ?? data.api_base_url ?? "http://localhost:8080";
    const duration = process.env.LOAD_TEST_DURATION
        ? parseInt(process.env.LOAD_TEST_DURATION, 10)
        : data.duration;
    const maxUsers = process.env.LOAD_TEST_MAX_USERS
        ? parseInt(process.env.LOAD_TEST_MAX_USERS, 10)
        : data.max_users;
    const startUsers = process.env.LOAD_TEST_START_USERS
        ? parseInt(process.env.LOAD_TEST_START_USERS, 10)
        : data.start_users;
    return new Config(
        data.num_users,
        data.weights ?? {},
        data.think_time ?? 1000,
        startUsers,
        maxUsers,
        data.ramp_up_time,
        data.ramp_down_time ?? 0,
        duration,
        data.transition_weights,
        data.statistics_recorded ?? [],
        apiBaseUrl,
    );
}
