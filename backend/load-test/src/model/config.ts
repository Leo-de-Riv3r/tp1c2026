export class Config {

    num_users: number;
    weights: Record<string, number>;
    think_time: number;
    start_users: number;
    max_users: number;
    ramp_up_time: number;
    ramp_down_time: number;
    duration: number;
    transition_weights: Record<string, number>;
    statistics_recorded: string[];
    api_base_url: string;
    
    constructor(num_users: number, weights: Record< string, number>, think_time: number, start_users: number, max_users: number, ramp_up_time: number, ramp_down_time: number, duration: number, transition_weights: Record<string, number>, statistics_recorded: string[], api_base_url: string) {
        this.num_users = num_users;
        this.weights = weights;
        this.think_time = think_time;
        this.start_users = start_users;
        this.max_users = max_users;
        this.ramp_up_time = ramp_up_time;
        this.ramp_down_time = ramp_down_time;
        this.duration = duration;
        this.transition_weights = transition_weights;
        this.statistics_recorded = statistics_recorded;
        this.api_base_url = api_base_url;
    }


}