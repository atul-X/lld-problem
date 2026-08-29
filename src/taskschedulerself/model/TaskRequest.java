package taskschedulerself.model;

import java.util.concurrent.Callable;

public class TaskRequest {
    private final String name;
    private final Callable<Void> job;
    private final long initialDelayMillis;
    private final RetryPolicy retryPolicy;
    private final RecurrenceStrategy recurrenceStrategy;

    public TaskRequest(Builder builder) {
        this.name = builder.name;
        this.job = builder.job;
        this.initialDelayMillis = builder.initialDelayMillis;
        this.retryPolicy = builder.retryPolicy;
        this.recurrenceStrategy = builder.recurrenceStrategy;
    }

    public String getName() {
        return name;
    }

    public Callable<Void> getJob() {
        return job;
    }

    public long getInitialDelayMillis() {
        return initialDelayMillis;
    }

    public RetryPolicy getRetryPolicy() {
        return retryPolicy;
    }

    public RecurrenceStrategy getRecurrenceStrategy() {
        return recurrenceStrategy;
    }

    public static class Builder {
        private String name = "task";
        private Callable<Void> job;
        private long initialDelayMillis = 0;
        private RetryPolicy retryPolicy = new NoRetry();
        private RecurrenceStrategy recurrenceStrategy = new OneTimeStrategy();

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder job(Callable<Void> job) {
            this.job = job;
            return this;
        }

        public Builder initialDelayMillis(long initialDelayMillis) {
            this.initialDelayMillis = initialDelayMillis;
            return this;
        }

        public Builder retryPolicy(RetryPolicy retryPolicy) {
            this.retryPolicy = retryPolicy;
            return this;
        }

        public Builder recurrenceStrategy(RecurrenceStrategy recurrenceStrategy) {
            this.recurrenceStrategy = recurrenceStrategy;
            return this;
        }

        public TaskRequest build() {
            if (job == null) {
                throw new IllegalStateException("job is required");
            }
            if (initialDelayMillis < 0) {
                throw new IllegalStateException("initialDelayMillis cannot be negative");
            }
            return new TaskRequest(this);
        }
    }
}
