package org.jurassicraft.common.entity.ai.util;

import net.minecraft.util.BlockPos;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jurassicraft.common.entity.base.EntityDinosaur;

import java.util.*;

/**
 * Copyright 2016 TimelessModdingTeam
 */
public class HerdManager
{
    // Blocks
    public static final int MIN_SIZE = 3;
    public static final long REBALANCE_DELAY_TICKS = 200;

    private static final Logger LOGGER = LogManager.getLogger();

    private static HerdManager instance;

    // For now we have one Herd per class of critter.  This needs to change when we have
    // different herds because of different herd leaders.
    private final Map<Class, Herd> herds = new HashMap<Class, Herd>();
    private long nextRebalance = 0;

    //===============================================

    // Returns the current instance of the herd manager
    public static HerdManager getInstance()
    {
        if (instance == null)
        {
            instance = new HerdManager();
        }

        return instance;
    }

    /**
     * Adds a dinosaur to the herd manager.
     *
     * @param dinosaur The dinosaur to add.
     */
    public void add(EntityDinosaur dinosaur)
    {
        Herd herd = herds.get(dinosaur.getClass());
        if (herd == null)
        {
            herd = new Herd();
            herds.put(dinosaur.getClass(), herd);
        }

        herd.add(dinosaur);
    }

    /**
     * Removes the dinosaur from the herd manager.
     *
     * @param dinosaur The dinosaur to remove.
     */
    public void remove(EntityDinosaur dinosaur)
    {
        Herd species = herds.get(dinosaur.getClass());
        if (species != null)
        {
            species.remove(dinosaur);
        }
    }

    /**
     * Provides the herd for the particular dinosaur.
     * @param dinosaur The dino.
     * @return The herd.
     */
    public Herd getHerd(EntityDinosaur dinosaur)
    {
        return herds.get(dinosaur.getClass());
    }

    /**
     * Returns the location this dinosaur is supposed to move to either within
     * the herd it is already in, or to a herd nearby.
     *
     * @param dinosaur The dinosaur we want to find the destination for.
     * @return The block pos to move to, or null for stay in place.
     */
    public BlockPos getWanderLocation(EntityDinosaur dinosaur)
    {
        // TODO:  Move the rebalance check to a general update tick handler
        long now = dinosaur.getEntityWorld().getTotalWorldTime();
        if (now > nextRebalance)
        {
            rebalanceAll();
            nextRebalance = now + REBALANCE_DELAY_TICKS;
        }

        // Basic design:
        // 1) Ask the herd for the center of the appropriate cluster
        // 2) See if we are within the "area of the cluster."  Note, this is expected to change
        //    based on cluster state ( e.g. IDLE, PANIC )
        // 3) If not within area of the cluster, compute an interest point based
        //    the species.

        Herd herd = herds.get(dinosaur.getClass());
        if (herd != null)
        {
            Cluster cluster = herd.getCluster(dinosaur);

            if (cluster == null)
            {
                // Not inside a cluster, let's find one to move to.
                cluster = herd.getLargestCluster();
                LOGGER.info("Found largest.");
            }

            if (cluster != null)
            {
                // Note, the outer radius might change over time.
                int outerRadius = cluster.getOuterRadius(dinosaur);
                BlockPos center = cluster.getCenter();

                // Okay, we want to move closer.  Let's just move into radius
                int distanceToCenter = (int) Math.sqrt(center.distanceSq(dinosaur.getPosition()));
                if (distanceToCenter < outerRadius)
                {
                    return null;
                }

                int diffDist = distanceToCenter - outerRadius;
                BlockPos target = AIUtils.computePosToward(dinosaur.getPosition(), center, diffDist);
                LOGGER.info("id=" + dinosaur.getEntityId() + ", start=" + dinosaur.getPosition() + ", center=" + center + ", target=" + target +
                            ", dtc=" + distanceToCenter + ", outer=" + outerRadius + ", diff=" + diffDist);
                return target;
            }

            LOGGER.info("no cluster.");
            return null;
        }

        return null;
    }

    /**
     * Updates all the herds in the list.
     */
    public void rebalanceAll()
    {
        LOGGER.info("!!! Rebalancing");
        for (Herd species : herds.values())
        {
            species.rebalance();
        }

        LOGGER.info(this);
    }

    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<Class, Herd> herd : herds.entrySet())
        {
            builder.append("[ species=" + herd.getKey().getSimpleName() +
                    ", clusters=" + herd.getValue().numClusters() +
                    ", noise=" + herd.getValue().noiseSize() +
                    " ]\n");
            if (herd.getValue().numClusters() > 0)
            {
                builder.append("  [ ");
                for (Cluster cluster : herd.getValue()._clusters)
                {
                    builder.append(" #:" + cluster._dinosaurs.size());
                    for (EntityDinosaur dino : cluster._dinosaurs)
                    {
                        builder.append(", ").append(dino.getPosition());
                    }
                }
                builder.append(" ]\n");
            }
            builder.append("  noise=[ ");
            for (EntityDinosaur dino : herd.getValue()._noise)
            {
                builder.append(", ").append(dino.getPosition());
            }
            builder.append(" ]\n");
        }
        return builder.toString();
    }

    //=============================================================================================

    // Takes care of all the members of a species.
    public class Herd
    {
        private LinkedList<Cluster> _clusters = new LinkedList<Cluster>();
        private LinkedList<EntityDinosaur> _noise = new LinkedList<EntityDinosaur>();
        private final LinkedList<EntityDinosaur> _allDinosaurs = new LinkedList<EntityDinosaur>();

        /**
         * Returns the cluster this dinosaur is in, or null if not in a cluster.
         * @param dinosaur The dinosaur we are looking for.
         * @return Cluster or null.
         */
        public Cluster getCluster(EntityDinosaur dinosaur)
        {
            for (Cluster cluster : _clusters)
            {
                if (cluster.contains(dinosaur))
                {
                    return cluster;
                }
            }
            return null;
        }

        /**
         * @return Returns the largest cluster.
         */
        public Cluster getLargestCluster()
        {
            Cluster largest = null;
            for (Cluster cluster : _clusters)
            {
                if (largest == null || cluster.size() > largest.size())
                {
                    largest = cluster;
                }
            }
            return largest;
        }

        /**
         * @return Returns the nearest cluster. Note, this computationally expensive.
         */
//        public Cluster getNearestCluster(EntityDinosaur dinosaur)
//        {
//
//        }


        // =======================

        void add(EntityDinosaur dinosaur)
        {
            _allDinosaurs.add(dinosaur);

            // Add to a cluster if we can find one.
            LinkedList<Cluster> belongsTo = new LinkedList<Cluster>();
            for (Cluster cluster : _clusters)
            {
                if (cluster.withinProximity(dinosaur))
                {
                    belongsTo.add(cluster);
                }
            }

            // If we didn't find anything, then add it to noise
            if (belongsTo.size() == 0)
            {
                //LOGGER.info("Added to noise: " + dinosaur.getClass().getSimpleName());
                _noise.add(dinosaur);
                return;
            }

            // Merge them if we have more than one
            Cluster main = belongsTo.poll();
            Cluster toMerge;
            while ((toMerge = belongsTo.poll()) != null)
            {
                main.mergeFrom(toMerge);
                _clusters.remove(toMerge);
            }
        }

        void remove(EntityDinosaur dinosaur)
        {
            _allDinosaurs.remove(dinosaur);

            for (Cluster cluster : _clusters)
            {
                if (cluster.remove(dinosaur))
                {
                    return;
                }
            }
        }

        BlockPos getClusterCenter(EntityDinosaur dinosaur)
        {
            for (Cluster cluster : _clusters)
            {
                if (cluster.contains(dinosaur))
                {
                    return cluster.getCenter();
                }
            }

            // Choose the largest
            Cluster largest = null;
            for (Cluster cluster : _clusters)
            {
                if (largest == null || cluster.size() > largest.size())
                {
                    largest = cluster;
                }
            }

            return (largest != null) ? largest.getCenter() : null;
        }

        int numClusters()
        {
            return _clusters.size();
        }

        int noiseSize()
        {
            return _noise.size();
        }

        void rebalance()
        {
            // For now this is a hack!  We recompute the entire thing instead of just updating
            _noise = new LinkedList<EntityDinosaur>(_allDinosaurs);
            _clusters = cluster(_noise);

            //LOGGER.info("After rebalance #clusters: " + _clusters.size());

            // Now, prune out all the tiny clusters
            Iterator<Cluster> iter = _clusters.iterator();
            while (iter.hasNext())
            {
                Cluster cluster = iter.next();
                if (cluster.size() < MIN_SIZE)
                {
                    //LOGGER.info("Merging in cluster of size: " + cluster.size());
                    cluster.drainTo(_noise);
                    iter.remove();
                }
            }
        }
    }

    //=============================================================================================

    // Manages a set of these
    public class Cluster
    {
        // We use linked list because we traverse and don't need random access
        private LinkedList<EntityDinosaur> _dinosaurs = new LinkedList<EntityDinosaur>();


        /**
         * Gets the "center" (average) of the cluster,
         *
         * @return The center.
         */
        public BlockPos getCenter()
        {
            double totalX = 0.0F;
            double totalY = 0.0F;
            double totalZ = 0.0F;
            int count = 0;

            for (EntityDinosaur dino : _dinosaurs)
            {
                BlockPos pos = dino.getPosition();
                totalX += pos.getX();
                totalY += pos.getY();
                totalZ += pos.getZ();
                count += 1;
            }

            return new BlockPos(totalX / count, totalY / count, totalZ / count);
        }

        /**
         * @return The desired outer radius from the cluster.
         */
        public int getOuterRadius(EntityDinosaur dinosaur)
        {
            double factor = 1 + Math.sqrt(size());
            return (int) Math.round(dinosaur.width * factor);
        }

        //=====================================================

        /**
         * Is the dinosaur withinProximity to other dinosaurs?
         *
         * @param dinosaur The dinosaur in question.
         * @return True if withinProximity to a cluster.
         */
        boolean withinProximity(EntityDinosaur dinosaur)
        {
            for (EntityDinosaur entity : _dinosaurs)
            {
                if (inProximity(dinosaur, entity))
                {
                    return true;
                }
            }
            return false;
        }

        /**
         * Is the dinosaur in the cluster?
         *
         * @param dinosaur The dinosaur.
         * @return True if in the list.
         */
        boolean contains(EntityDinosaur dinosaur)
        {
            return _dinosaurs.contains(dinosaur);
        }


        /**
         * The dinosaur to the cluster.
         *
         * @param dinosaur The dinosaur to add.
         */
        void add(EntityDinosaur dinosaur)
        {
            _dinosaurs.add(dinosaur);
        }

        /**
         * Remove the element if it exists.
         *
         * @param dinosaur The element to remove.
         * @return True if removed.
         */
        boolean remove(EntityDinosaur dinosaur)
        {
            return _dinosaurs.remove(dinosaur);
        }

        // Bring in the content from another cluster
        void mergeFrom(Cluster other)
        {
            _dinosaurs.addAll(other._dinosaurs);
            other._dinosaurs.clear();
        }

        int size()
        {
            return _dinosaurs.size();
        }

        void drainTo(Collection<EntityDinosaur> sink)
        {
            sink.addAll(_dinosaurs);
            _dinosaurs.clear();
        }

        void addWithAdjacents(EntityDinosaur dinosaur, LinkedList<EntityDinosaur> dinosaurs)
        {
            // Recursively add all adjacents
            List<EntityDinosaur> allProximates = extractProximates(dinosaur, dinosaurs);
            _dinosaurs.add(dinosaur);
            for (EntityDinosaur close : allProximates)
            {
                addWithAdjacents(close, dinosaurs);
            }
        }

    }

    //===============================================

    private LinkedList<Cluster> cluster(LinkedList<EntityDinosaur> dinosaurs)
    {
        EntityDinosaur dino;
        LinkedList<Cluster> clusters = new LinkedList<Cluster>();

        while ((dino = dinosaurs.poll()) != null)
        {
            Cluster cluster = new Cluster();
            clusters.add(cluster);
            cluster.addWithAdjacents(dino, dinosaurs);
        }

        return clusters;
    }

    private LinkedList<EntityDinosaur> extractProximates(EntityDinosaur dinosaur, LinkedList<EntityDinosaur> dinosaurs)
    {
        LinkedList<EntityDinosaur> proximates = new LinkedList<EntityDinosaur>();
        Iterator<EntityDinosaur> iter = dinosaurs.iterator();
        while (iter.hasNext())
        {
            EntityDinosaur tmp = iter.next();
            if (inProximity(dinosaur, tmp))
            {
                proximates.add(tmp);
                iter.remove();
            }
        }

        //LOGGER.info("extractProximates inDinos=" + dinosaurs.size() + ", extracted=" + proximates.size() );
        return proximates;
    }

    //=============================================================================================

    // Used for the
    private static boolean inProximity(EntityDinosaur lhs, EntityDinosaur rhs)
    {
        return lhs.getPosition().distanceSq(rhs.getPosition()) < speciesDistanceSq(lhs);
    }

    // Basic species distance
    private static int speciesDistance(EntityDinosaur dinosaur)
    {
        // Microceratus - 0.4 - So, micro's need to be 1.4 blocks away to make a cluster!
        // Apatosaurus - 6.5 -
        double radius = dinosaur.width * 3;
        if (radius < 4)
        {
            radius = 4;
        }
        return (int) Math.round(radius);
    }

    // Square of basic distance
    private static int speciesDistanceSq(EntityDinosaur dinosaur)
    {
        double radius = dinosaur.width * 3;
        return (int) Math.round(radius * radius);
    }

}

/*
    - For cluster they are in proximity
    - When moving toward they move some number of points
    - When wandering, only move so far away
 */

/*
    Performance guidelines
    - We expect more rebalancing than addition removal, so maximize for that
 */

/*

We use DBSCAN to find clusters.

====

DBSCAN(D, eps, MinPts) {
   C = 0
   for each point P in dataset D {
      if P is visited
         continue next point
      mark P as visited
      NeighborPts = regionQuery(P, eps)
      if sizeof(NeighborPts) < MinPts
         mark P as NOISE
      else {
         C = next cluster
         expandCluster(P, NeighborPts, C, eps, MinPts)
      }
   }
}

expandCluster(P, NeighborPts, C, eps, MinPts) {
   add P to cluster C
   for each point P' in NeighborPts {
      if P' is not visited {
         mark P' as visited
         NeighborPts' = regionQuery(P', eps)
         if sizeof(NeighborPts') >= MinPts
            NeighborPts = NeighborPts joined with NeighborPts'
      }
      if P' is not yet member of any cluster
         add P' to cluster C
   }
}

regionQuery(P, eps)
   return all points within P's eps-neighborhood (including P)

=================================

The tricky bits is when updating.

Is there an easy way to merge clusters?  Can we start with cluster that isn't


 */