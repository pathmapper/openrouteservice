/*
 *  Licensed to GraphHopper GmbH under one or more contributor
 *  license agreements. See the NOTICE file distributed with this work for
 *  additional information regarding copyright ownership.
 *
 *  GraphHopper GmbH licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except in
 *  compliance with the License. You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.heigit.ors.fastisochrones.partitioning;

/**
 * Lightweight entry object for ordering nodes in a queue/deque based on weight.
 *
 * @author Hendrik Leuschner
 */
public class EKEdgeEntry implements Comparable<EKEdgeEntry> {
    public int node;
    public int weight;

    public EKEdgeEntry(int node, int weight) {
        this.node = node;
        this.weight = weight;
    }

    @Override
    public int compareTo(EKEdgeEntry o) {
        if (weight < o.weight)
            return -1;

        // assumption no NaN and no -0
        return weight > o.weight ? 1 : 0;
    }
}