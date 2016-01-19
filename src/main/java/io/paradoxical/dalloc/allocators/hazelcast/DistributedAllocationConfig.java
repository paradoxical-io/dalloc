package io.paradoxical.dalloc.allocators.hazelcast;

import lombok.Data;

@Data
public class DistributedAllocationConfig {
    private final long lockWaitSeconds;
}
