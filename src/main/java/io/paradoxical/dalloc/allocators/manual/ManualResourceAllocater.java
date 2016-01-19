package io.paradoxical.dalloc.allocators.manual;

import com.godaddy.logging.Logger;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import io.paradoxical.dalloc.ResourceAllocator;
import io.paradoxical.dalloc.model.ResourceConfig;
import io.paradoxical.dalloc.model.ResourceIdentity;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static com.godaddy.logging.LoggerFactory.getLogger;
import static java.util.stream.Collectors.toSet;

public class ManualResourceAllocater implements ResourceAllocator {

    private Logger logger = getLogger(ManualResourceAllocater.class);

    private final ManualAllocationConfig allocationConfig;
    private final Supplier<Set<ResourceIdentity>> inputSupplier;
    private final Consumer<Set<ResourceIdentity>> onDistributed;

    @Inject
    public ManualResourceAllocater(
            ManualAllocationConfig allocationConfig,
            @Assisted ResourceConfig config,
            @Assisted Supplier<Set<ResourceIdentity>> inputSupplier,
            @Assisted Consumer<Set<ResourceIdentity>> onDistributed) {
        this.allocationConfig = allocationConfig;
        this.inputSupplier = inputSupplier;
        this.onDistributed = onDistributed;
        logger = logger.with("allocation-group", config.getGroup());
    }

    @Override
    public void claim() {
        final List<ResourceIdentity> resourceIdentities = new ArrayList<>(inputSupplier.get());

        resourceIdentities.sort((a, b) -> a.get().compareTo(b.get()));

        final Integer manualAllocatorInstanceNumber = allocationConfig.getManualAllocatorInstanceNumber();

        final Integer manualAllocatorsCount = allocationConfig.getManualAllocatorsCount();

        if (manualAllocatorInstanceNumber == null || manualAllocatorsCount == null) {
            logger.warn("Manual allocation strategy selected but alloctor count and allocator instance are not set! Cannot allocate resources");
            return;
        }

        if (manualAllocatorInstanceNumber > manualAllocatorsCount) {
            logger.with("instance-num", manualAllocatorInstanceNumber)
                  .with("allocator-count", manualAllocatorsCount)
                  .warn("Instance number is greater than total allocators. Not allocating");

            return;
        }


        long sliceSize = Double.valueOf(Math.ceil((resourceIdentities.size() / manualAllocatorsCount))).longValue();

        long start = sliceSize * manualAllocatorInstanceNumber;

        long end;

        if (manualAllocatorsCount - 1 == manualAllocatorInstanceNumber) {
            end = resourceIdentities.size();
        }
        else {
            end = sliceSize * (manualAllocatorInstanceNumber + 1);
        }

        final Set<ResourceIdentity> slice = resourceIdentities.stream()
                                                              .skip(start)
                                                              .limit(end - start)
                                                              .collect(toSet());

        onDistributed.accept(slice);
    }

    @Override
    public void close() throws Exception {

    }
}
