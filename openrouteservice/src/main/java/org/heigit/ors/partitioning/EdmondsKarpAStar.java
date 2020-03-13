package org.heigit.ors.partitioning;


import com.carrotsearch.hppc.IntHashSet;
import com.carrotsearch.hppc.IntObjectHashMap;
import com.graphhopper.routing.util.EdgeFilter;
import com.graphhopper.storage.GraphHopperStorage;
import io.swagger.models.auth.In;

import java.util.*;

import static org.heigit.ors.partitioning.FastIsochroneParameters.FLOW__SET_SPLIT_VALUE;


public class EdmondsKarpAStar extends AbstractMaxFlowMinCutAlgorithm {

    private IntObjectHashMap<EdgeInfo> prevMap;
    private int srcLimit;
    private int snkLimit;

    public EdmondsKarpAStar(GraphHopperStorage ghStorage, PartitioningData pData, EdgeFilter edgeFilter, boolean init) {
        super(ghStorage, pData, edgeFilter, init);
    }

    public EdmondsKarpAStar() {
    }

    public boolean getRemainingCapacity(int edgeId, int nodeId) {
        FlowEdgeData flowEdgeData = pData.getFlowEdgeData(edgeId, nodeId);
        return !flowEdgeData.flow;
    }

    public void augment(int edgeId, int baseId, int adjId) {
        FlowEdgeData flowEdgeData = pData.getFlowEdgeData(edgeId, baseId);
        flowEdgeData.flow = true;
        FlowEdgeData inverseFlowEdgeData = pData.getFlowEdgeData(flowEdgeData.inverse, adjId);
        inverseFlowEdgeData.flow = false;
        pData.setFlowEdgeData(edgeId, baseId, flowEdgeData);
        pData.setFlowEdgeData(flowEdgeData.inverse, adjId, inverseFlowEdgeData);
    }

    @Override
    public void flood() {
        int flow;
        prevMap = new IntObjectHashMap((int)Math.ceil(FLOW__SET_SPLIT_VALUE * nodes));
        srcLimit = (int) (FLOW__SET_SPLIT_VALUE * nodes);
        snkLimit = (int) ((1 - FLOW__SET_SPLIT_VALUE) * nodes);
        Deque<Integer> deque = new ArrayDeque<>(nodes / 2);
        addSrcNodesToDeque(deque);
        do {
            prevMap.clear();
            setUnvisitedAll();
            flow = bfs(deque);
            maxFlow += flow;

            if ((maxFlow > maxFlowLimit)) {
                maxFlow = Integer.MAX_VALUE;
                break;
            }
        } while (flow > 0);
        prevMap = null;
    }

    private int bfs(Deque<Integer> initialDeque) {
        Deque<Integer> deque = copyInitialDeque(initialDeque);
        int calls = srcLimit;
        int node;

        double maxBFSCalls = _graph.getBaseGraph().getAllEdges().length() * 2;
        double sizeFactor = ((double) nodeOrder.size()) / _graph.getBaseGraph().getNodes();
        maxBFSCalls = (int)Math.ceil(maxBFSCalls * sizeFactor) + nodeOrder.size() * 2;

        while (!deque.isEmpty()) {
            if(calls > maxBFSCalls)
                return 0;
            node = deque.pop();

            if(snkLimit < nodeOrder.get(node)){
                prevMap.put(snkNodeId, new EdgeInfo(getDummyEdgeId(), node, snkNodeId));
                //Early stop
                break;
            }

            _edgeIter = _edgeExpl.setBaseNode(node);
            //Iterate over normal edges
            TreeSet<EKEdgeEntry> set = new TreeSet<>(EKEdgeEntry::compareTo);
            while(_edgeIter.next()){
                if(!edgeFilter.accept(_edgeIter))
                    continue;
                calls++;
                int adj = _edgeIter.getAdjNode();
                int edge = _edgeIter.getEdge();

                if(adj == node)
                    continue;
                if(!nodeOrder.containsKey(adj))
                    continue;
                if ((getRemainingCapacity(edge, node))
                        && !isVisited(pData.getVisited(adj))) {
                    setVisited(adj);
                    prevMap.put(adj, new EdgeInfo(edge, node, adj));
                    set.add(new EKEdgeEntry(adj, this.nodeOrder.get(adj)));
                }
            }
            for(EKEdgeEntry ekEdgeEntry : set)
                deque.push(ekEdgeEntry.node);
            calls++;
        }

        if (prevMap.getOrDefault(snkNodeId, null) == null)
            return 0;
        int bottleNeck = Integer.MAX_VALUE;

        EdgeInfo edge = prevMap.getOrDefault(snkNodeId, null);
        edge = prevMap.getOrDefault(edge.baseNode, null);
        while (edge != null){
            if(nodeOrder.get(edge.baseNode) < srcLimit)
                break;
            bottleNeck = Math.min(bottleNeck, getRemainingCapacity(edge.edge, edge.baseNode) ? 1 : 0);
            if(bottleNeck == 0)
                return 0;
            augment(edge.getEdge(), edge.getBaseNode(), edge.getAdjNode());
            edge = prevMap.getOrDefault(edge.baseNode, null);
        }
        return bottleNeck;
    }

    private int addSrcNodesToDeque(Deque<Integer> deque){
        //Reverse insertion order to maximize offer performance
        int nodeNumber = 0;
        while(nodeNumber < srcLimit) {
            int node = orderedNodes.get(nodeNumber);
            deque.push(node);
            setVisited(node);
            nodeNumber++;
        }
        return srcLimit;
    }

    private Deque<Integer> copyInitialDeque(Deque<Integer> initialDeque){
        Deque<Integer> deque = new ArrayDeque<>(initialDeque);
        //Reverse insertion order to maximize offer performance
        int nodeNumber = srcLimit - 1;
        while(nodeNumber > 0) {
            int node = orderedNodes.get(nodeNumber);
            setVisited(node);
            nodeNumber--;
        }
        return deque;
    }

    private class EdgeInfo{
        int edge;
        int baseNode;
        int adjNode;
        EdgeInfo(int edge, int baseNode, int adjNode){
            this.edge = edge;
            this.baseNode = baseNode;
            this.adjNode = adjNode;
        }
        public int getEdge() {
            return edge;
        }
        public int getBaseNode(){
            return baseNode;
        }
        public int getAdjNode() {
            return adjNode;
        }
    }
}
