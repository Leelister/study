package org.rpc.rpc.load.balancer;

import org.rpc.remoting.api.Directory;
import org.rpc.remoting.api.channel.ChannelGroup;

import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 加权随机
 */
public class RandomLoadBalancer implements LoadBalancer {

    private RandomLoadBalancer() {}

    private static class InnerSingleton {
        static final RandomLoadBalancer RANDOM_LOAD_BALANCER = new RandomLoadBalancer();
    }

    public static RandomLoadBalancer instance() {
        return InnerSingleton.RANDOM_LOAD_BALANCER;
    }

    @Override
    public ChannelGroup select(CopyOnWriteArrayList<ChannelGroup> list, Directory directory) {
        if (list == null && list.size() == 0) {
            return null;
        }
        ChannelGroup[] channelGroups = (ChannelGroup[]) list.toArray();
        if (channelGroups.length == 1) {
            return channelGroups[0];
        }

        boolean sameWeight = true;

        for (int i = 1; i < channelGroups.length && sameWeight; i++) {
            sameWeight = (channelGroups[0].getWeight(directory) == channelGroups[i].getWeight(directory));
        }

        int sumWeight = 0;
        for (int i = 0; i < channelGroups.length; i++) {
            sumWeight += channelGroups[i].getWeight(directory);
        }

        Random random = ThreadLocalRandom.current();

        if (sameWeight) {
            return channelGroups[random.nextInt(channelGroups.length)];
        } else {
            int offset = random.nextInt(sumWeight);
            for (ChannelGroup channelGroup : channelGroups) {
                offset -= channelGroup.getWeight(directory);
                if (offset < 0) {
                    return channelGroup;
                }
            }
        }
        return channelGroups[random.nextInt(channelGroups.length)];
    }
    
}
