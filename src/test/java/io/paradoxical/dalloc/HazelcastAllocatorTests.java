package io.paradoxical.dalloc;

import com.hazelcast.config.Config;
import com.hazelcast.config.GroupConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import io.paradoxical.dalloc.allocators.hazelcast.DistributedAllocationConfig;
import io.paradoxical.dalloc.allocators.hazelcast.HazelcastResourceAllocater;
import io.paradoxical.dalloc.model.ResourceConfig;
import io.paradoxical.dalloc.model.ResourceGroup;
import io.paradoxical.dalloc.model.ResourceIdentity;
import lombok.Cleanup;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.toSet;
import static org.assertj.core.api.Assertions.assertThat;

public class HazelcastAllocatorTests extends TestBase {


    @Test
    public void test_member_joins_rebalancing_occurs() throws Exception {
        Set<ResourceIdentity> claimed1 = new HashSet<>();

        @Cleanup final ResourceAllocator allocator1 = getAllocator("test_member_joins_rebalancing_occurs",
                                                                   () -> getFromInt(100),
                                                                   c -> {
                                                                       claimed1.clear();
                                                                       claimed1.addAll(c);
                                                                   });

        allocator1.claim();
        assertThat(claimed1.size()).isEqualTo(100);

        Set<ResourceIdentity> claimed2 = new HashSet<>();

        @Cleanup final ResourceAllocator allocator2 = getAllocator("test_member_joins_rebalancing_occurs",
                                                                   () -> getFromInt(100),
                                                                   c -> {
                                                                       claimed2.clear();
                                                                       claimed2.addAll(c);
                                                                   });


        allocator1.claim();
        allocator2.claim();

        assertThat(claimed1.size()).isEqualTo(50);
        assertThat(claimed2.size()).isEqualTo(50);
    }

    @Test
    public void test_member_releases_invalid_resources() throws Exception {
        Set<ResourceIdentity> claimed1 = new HashSet<>();

        Set<ResourceIdentity> totalSet = getFromInt(100);

        @Cleanup final ResourceAllocator allocator1 = getAllocator("test_member_releases_invalid_resources",
                                                                   () -> totalSet,
                                                                   c -> {
                                                                       claimed1.clear();
                                                                       claimed1.addAll(c);
                                                                   });

        allocator1.claim();

        assertThat(claimed1).isEqualTo(totalSet);

        totalSet.clear();

        totalSet.add(ResourceIdentity.valueOf("foo"));

        allocator1.claim();

        assertThat(claimed1).isEqualTo(totalSet);
    }

    @Test
    public void test_member_leaves_rebalancing_occurs() throws Exception {
        Set<ResourceIdentity> claimed1 = new HashSet<>();

        @Cleanup final ResourceAllocator allocator1 = getAllocator("test_member_leaves_rebalancing_occurs",
                                                                   () -> getFromInt(100),
                                                                   c -> {
                                                                       claimed1.clear();
                                                                       claimed1.addAll(c);
                                                                   });

        Set<ResourceIdentity> claimed2 = new HashSet<>();

        @Cleanup final ResourceAllocator allocator2 = getAllocator("test_member_leaves_rebalancing_occurs",
                                                                   () -> getFromInt(100),
                                                                   c -> {
                                                                       claimed2.clear();
                                                                       claimed2.addAll(c);
                                                                   });

        allocator1.claim();
        allocator2.claim();

        assertThat(claimed1.size()).isEqualTo(50);
        assertThat(claimed2.size()).isEqualTo(50);

        allocator1.close();

        allocator2.claim();

        assertThat(claimed2).isEqualTo(getFromInt(100));
    }

    @Test
    public void test_single_member_claims_all() throws Exception {
        Set<ResourceIdentity> claimed = new HashSet<>();

        @Cleanup final ResourceAllocator allocator = getAllocator("test_single_member_claims_all",
                                                                  () -> getFromInt(100),
                                                                  claimed::addAll);

        allocator.claim();

        assertThat(claimed).isEqualTo(getFromInt(100));
    }

    private Set<ResourceIdentity> getFromInt(int max) {
        return IntStream.range(0, max).mapToObj(i -> new ResourceIdentity(String.valueOf(i))).collect(toSet());
    }

    private ResourceAllocator getAllocator(String name, Supplier<Set<ResourceIdentity>> input, Consumer<Set<ResourceIdentity>> onClaim) {
        return new ResourceAllocator() {
            private final HazelcastInstance instance = getInstance(name);

            private final HazelcastResourceAllocater allocater = new HazelcastResourceAllocater(instance,
                                                                                                new DistributedAllocationConfig(15L),
                                                                                                new ResourceConfig(ResourceGroup.valueOf(name)),
                                                                                                input,
                                                                                                onClaim);

            @Override
            public void claim() {
                allocater.claim();
            }

            @Override
            public void close() throws Exception {
                allocater.close();

                instance.shutdown();
            }
        };
    }

    private HazelcastInstance getInstance(String clusterName) {
        Config config = new Config();
        config.setInstanceName(clusterName + "_" + UUID.randomUUID().toString());
        config.setGroupConfig(new GroupConfig(clusterName));
        config.getNetworkConfig().getJoin().getMulticastConfig().setEnabled(false);
        config.getNetworkConfig().getJoin().getTcpIpConfig().setEnabled(true);
        config.getNetworkConfig().getJoin().getTcpIpConfig().addMember("127.0.0.1");
        config.getNetworkConfig().getInterfaces().clear();
        config.getNetworkConfig().getInterfaces().addInterface("127.0.0.*");
        config.getNetworkConfig().getInterfaces().setEnabled(true);

        return Hazelcast.newHazelcastInstance(config);
    }
}


