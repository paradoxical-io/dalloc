paradoxical.dalloc
========================

![Build status](https://travis-ci.org/paradoxical-io/dalloc.svg?branch=master)

A distributed resource allocator. It supports

- Hazelcast distribution
- Manual distribution

# What does it solve?

The problem we are solving is having many distributed machines who should act on only 1 resource out of a set of resources.  For example, you have 10,000 items that each need monitoring, and you don't want more than one machine 
monitoring an element in the set.  You need to distribute this work to a cluster of boxes such that each box is monitoring `10,000/clusterSize` elements.  

# Hazelcast Distribution

Distribution in hazelcast constitutes of mapping your input set of resources to a `ResourceIdentifier` and grouping them as part of a `ResourceGroup`.  Given the set of
members in the hazelcast instance the resources are distributed evenly to each member.  As members join the cluster they may rebalance resources (i.e. give up some resources they had
so that new members can claim them).  As members leave the cluster, members will rebalance the required load.

## Claiming resources

Claim event can occur by either

- A member joining
- A member leaving
- Manually invoked

The claim function will be dispatched on member join/leave events and is executed on the same thread.  If you need to do long running work during a claim event it is best to capture the Set of claimed resources
and dispatch your work on a threadpool.

## Split brain

There is no splitbrain merge exposed here. 

## How it works

There is a single `Map<ClusterMember, Set<ResourceIdentifier>>` stored on a distributed node (to minimize cluster IO) that each member tries to lock and acquire.  When someone acquires a lock
they find out the intersection of the total vs already allocated (owned by the set) and divies up the required needing allocation

```
Needing Acquisition = Total available ∩ Already allocated
```
 
When a member leaves, all members will try and acquire the lock and prune that members resources.
 
# Manual distribution

Manual distribution relies on the user to define the total expected set of workers and to manually label each worker.  Each worker will then grab 

```
Total Set / Workers
```

Resources.  There is no guarantee of overlap protection, but since resource identifiers are _ordered_ they will deterministically choose the same slice sections. 
