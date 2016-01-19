package io.paradoxical.dalloc;


import io.paradoxical.dalloc.allocators.manual.ManualAllocationConfig;
import io.paradoxical.dalloc.allocators.manual.ManualResourceAllocater;
import io.paradoxical.dalloc.model.ResourceConfig;
import io.paradoxical.dalloc.model.ResourceIdentity;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.toSet;
import static org.assertj.core.api.Assertions.assertThat;


@RunWith(Parameterized.class)
public class ManualAllocatorTests extends TestBase {

    private final int instanceNumber;
    private final int clusterSize;
    private final int totalAllocatable;
    private final Integer[] expectedResult;

    public ManualAllocatorTests(int instanceNumber, int clusterSize, int totalAllocatable, Integer[] expectedResult) {
        this.instanceNumber = instanceNumber;
        this.clusterSize = clusterSize;
        this.totalAllocatable = totalAllocatable;
        this.expectedResult = expectedResult;
    }

    @Parameterized.Parameters(name = "Instance {0}, ClusterSize {1}, TotalToDistribute {2}")
    public static List<Object[]> data() {
        return Arrays.asList(new Object[][]{
                { 0, 3, 10, new Integer[]{ 0, 1, 2, } },
                { 1, 3, 10, new Integer[]{ 3, 4, 5, } },
                { 2, 3, 10, new Integer[]{ 6, 7, 8, 9 } }
        });
    }

    @Test
    public void test_passthrough_allocation() {
        final ManualAllocationConfig allocationConfig = new ManualAllocationConfig();

        allocationConfig.setManualAllocatorsCount(clusterSize);

        allocationConfig.setManualAllocatorInstanceNumber(instanceNumber);

        Set<Integer> allocated = allocate(totalAllocatable, allocationConfig);

        assertThat(allocated.toArray()).isEqualTo(expectedResult);
    }

    private Set<Integer> allocate(final int totalResources, final ManualAllocationConfig manualAllocationConfig) {
        final Set<ResourceIdentity> inputData = IntStream.range(0, totalResources).mapToObj(i -> new ResourceIdentity(String.valueOf(i))).collect(toSet());

        final Set<ResourceIdentity> allocatedData = new HashSet<>();

        final ResourceAllocator allocator = new ManualResourceAllocater(manualAllocationConfig,
                                                                        fixture.manufacturePojo(ResourceConfig.class),
                                                                        () -> inputData,
                                                                        allocatedData::addAll);


        allocator.claim();

        return allocatedData.stream().map(i -> Integer.valueOf(i.get())).collect(Collectors.toSet());
    }
}