import { createConfig } from "./factories/config_factory.js";
import { createUsers } from "./factories/user_factory.js";
import { LoadRunnerApi } from "./api/load_runner_api.js";
import { Runner } from "./model/runner.js";
import "./data/states.js";

const config = createConfig("config.json");
const users = createUsers("users.json");
const api = new LoadRunnerApi(config);

console.log(`Starting load test: ${users.length} users, ${config.duration}s duration`);
const runner = new Runner(api, users, config);
await runner.execute();
api.metrics.summary();
