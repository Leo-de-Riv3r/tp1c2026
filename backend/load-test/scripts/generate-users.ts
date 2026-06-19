import fs from "fs";
import path from "path";
import { fileURLToPath } from "url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const root = path.resolve(__dirname, "..");

interface SeedConfig {
    userCount: number;
    password: string;
    emailPrefix: string;
    emailDomain: string;
    thinkTime: number;
}

function loadConfig(): SeedConfig {
    const configPath = path.join(root, "seed", "config.json");
    const raw = JSON.parse(fs.readFileSync(configPath, "utf-8"));
    const userCount = parseInt(process.env.LOAD_TEST_USER_COUNT ?? String(raw.userCount), 10);
    return {
        userCount,
        password: raw.password,
        emailPrefix: raw.emailPrefix,
        emailDomain: raw.emailDomain,
        thinkTime: raw.thinkTime,
    };
}

function generateUsers(config: SeedConfig) {
    const users = [];
    for (let i = 0; i < config.userCount; i++) {
        users.push({
            email: `${config.emailPrefix}${i}@${config.emailDomain}`,
            password: config.password,
            think_time: config.thinkTime,
            available_cards: [],
            missing_cards: [],
        });
    }
    return users;
}

function syncConfigJson(userCount: number) {
    const configPath = path.join(root, "config.json");
    const config = JSON.parse(fs.readFileSync(configPath, "utf-8"));
    config.num_users = userCount;
    config.max_users = userCount;
    config.start_users = Math.min(config.start_users ?? 1, userCount);
    fs.writeFileSync(configPath, JSON.stringify(config, null, 2) + "\n");
}

const config = loadConfig();
const users = generateUsers(config);
const usersPath = path.join(root, "users.json");
fs.writeFileSync(usersPath, JSON.stringify(users, null, 2) + "\n");
syncConfigJson(config.userCount);
console.log(`Generated ${users.length} users -> ${usersPath}`);
console.log(`Updated config.json: num_users=${config.userCount}, max_users=${config.userCount}`);
