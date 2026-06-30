export class Trend {
    private values: number[] = [];

    add(value: number): void {
        this.values.push(value);
    }

    count(): number {
        return this.values.length;
    }

    avg(): number {
        if (this.values.length === 0) return 0;
        return this.values.reduce((a, b) => a + b, 0) / this.values.length;
    }

    min(): number {
        if (this.values.length === 0) return 0;
        return Math.min(...this.values);
    }

    max(): number {
        if (this.values.length === 0) return 0;
        return Math.max(...this.values);
    }

    p95(): number {
        if (this.values.length === 0) return 0;
        const sorted = [...this.values].sort((a, b) => a - b);
        const idx = Math.ceil(sorted.length * 0.95) - 1;
        return sorted[Math.max(0, idx)];
    }
}

export class Counter {
    private value = 0;

    add(n = 1): void {
        this.value += n;
    }

    get(): number {
        return this.value;
    }
}

export class Rate {
    private successes = 0;
    private failures = 0;

    add(success: boolean): void {
        if (success) this.successes++;
        else this.failures++;
    }

    rate(): number {
        const total = this.successes + this.failures;
        if (total === 0) return 0;
        return this.failures / total;
    }

    get successesCount(): number {
        return this.successes;
    }

    get failuresCount(): number {
        return this.failures;
    }
}

export class MetricsRegistry {
    httpReqDuration = new Trend();
    httpReqFailed = new Rate();
    private endpointTrends = new Map<string, Trend>();

    recordRequest(endpoint: string, durationMs: number, success: boolean): void {
        this.httpReqDuration.add(durationMs);
        this.httpReqFailed.add(success);
        let trend = this.endpointTrends.get(endpoint);
        if (!trend) {
            trend = new Trend();
            this.endpointTrends.set(endpoint, trend);
        }
        trend.add(durationMs);
    }

    summary(): void {
        console.log('\n=== Load Test Metrics ===');
        console.log(`http_req_duration: avg=${this.httpReqDuration.avg().toFixed(2)}ms min=${this.httpReqDuration.min().toFixed(2)}ms max=${this.httpReqDuration.max().toFixed(2)}ms p95=${this.httpReqDuration.p95().toFixed(2)}ms (n=${this.httpReqDuration.count()})`);
        console.log(`http_req_failed: rate=${(this.httpReqFailed.rate() * 100).toFixed(2)}% (${this.httpReqFailed.failuresCount} failed / ${this.httpReqFailed.successesCount + this.httpReqFailed.failuresCount} total)`);
        if (this.endpointTrends.size > 0) {
            console.log('\nPer-endpoint durations:');
            for (const [endpoint, trend] of [...this.endpointTrends.entries()].sort((a, b) => a[0].localeCompare(b[0]))) {
                console.log(`  ${endpoint}: avg=${trend.avg().toFixed(2)}ms p95=${trend.p95().toFixed(2)}ms (n=${trend.count()})`);
            }
        }
        console.log('=========================\n');
    }
}
