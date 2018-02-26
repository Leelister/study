package org.rpc.rpc.load.balancer;

import org.rpc.remoting.api.Directory;
import org.rpc.remoting.api.channel.ChannelGroup;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class RandomRobinLoadBalancer implements LoadBalancer {

    private AtomicInteger integer = new AtomicInteger(0);

    private RandomRobinLoadBalancer() {}

    public static RandomRobinLoadBalancer instance() {
        return new RandomRobinLoadBalancer();
    }
    
    @Override
    public ChannelGroup select(CopyOnWriteArrayList<ChannelGroup> list, Directory directory) {
        if (list == null && list.size() == 0) {
            return null;
        }
        ChannelGroup[] channelGroups = new ChannelGroup[list.size()];
        channelGroups = list.toArray(channelGroups);

        if (channelGroups.length == 1) {
            return channelGroups[0];
        }

        boolean sameWeight = true;
        for (int i = 1; i < channelGroups.length && sameWeight; i++) {
            sameWeight = (channelGroups[0].getWeight(directory) == channelGroups[i].getWeight(directory));
        }

        int index = integer.getAndIncrement() & Integer.MAX_VALUE;
        if (sameWeight) {
            return channelGroups[index % channelGroups.length];
        } else {
            int commonDivisor = commonDivisor(directory, channelGroups);
            int sumWeight = 0;
            for (int i = 0; i < channelGroups.length; i++) {
                sumWeight += channelGroups[i].getWeight(directory);
            }
            // sumWeight / commonDivisor -> 一轮多少次
            int offset = index % (sumWeight / commonDivisor);
            int weight = 0;
            for (int i = 0; i < channelGroups.length; i++) {
                weight += channelGroups[i].getWeight(directory);
                if (offset * commonDivisor < weight) {
                    return channelGroups[i];
                }
            }
        }
        return channelGroups[index % channelGroups.length];
        
    }
    
    // 求最大公约数
    private int commonDivisor(Directory directory, ChannelGroup... channelGroups) {
        int commonDivisor = gcd(channelGroups[0].getWeight(directory), channelGroups[1].getWeight(directory));
        for (int i = 1; i < channelGroups.length - 1; i++) {
            commonDivisor = gcd(commonDivisor, channelGroups[i].getWeight(directory));
        }
        return commonDivisor;
    }

    private int gcd(int a, int b) {
        if (a < b) {
            a = a ^ b;
            b = b ^ a;
            a = a ^ b;
        }
        if (a % b == 0)
            return b;
        else
            return gcd(b, a % b);
    }
}
