/*
 * Copyright 2013 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.maps.android.quadtree;

import com.google.maps.android.geometry.Bounds;
import com.google.maps.android.geometry.Point;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * A quad tree which tracks items with a Point geometry.
 * See http://en.wikipedia.org/wiki/Quadtree for details on the data structure.
 * This class is not thread safe.
 */
public class PointQuadTree<T extends QuadTree.Item> implements QuadTree<T> {

    /**
     * The bounds of this quad.
     */
    private final Bounds mBounds;

    /**
     * The depth of this quad in the tree.
     */
    private final int mDepth;

    /**
     * Maximum number of elements to store in a quad before splitting.
     */
    private final static int MAX_ELEMENTS = 40;

    /**
     * The elements inside this quad, if any.
     */
    private List<T> mItems;

    /**
     * Maximum depth.
     */
    private final static int MAX_DEPTH = 40;

    /**
     * Child quads.
     */
    private PointQuadTree<T>[] mChildren = null;

    /**
     * Creates a new quad tree with specified bounds.
     *
     * @param minX
     * @param maxX
     * @param minY
     * @param maxY
     */
    public PointQuadTree(double minX, double maxX, double minY, double maxY) {
        this(new Bounds(minX, maxX, minY, maxY));
    }

    public PointQuadTree(Bounds bounds) {
        this(bounds, 0);
    }

    private PointQuadTree(double minX, double maxX, double minY, double maxY, int depth) {
        this(new Bounds(minX, maxX, minY, maxY), depth);
    }

    private PointQuadTree(Bounds bounds, int depth) {
        mBounds = bounds;
        mDepth = depth;
    }

    /**
     * Insert an item.
     */
    @Override
    public void add(T item) {
        Point point = item.getPoint();
        if (this.mBounds.contains(point.x, point.y)) {
            insert(point.x, point.y, item);
        }
    }

    private void insert(double x, double y, T item) {
        if (this.mChildren != null) {
            if (y < mBounds.midY) {
                if (x < mBounds.midX) { // top left
                    mChildren[0].insert(x, y, item);
                } else { // top right
                    mChildren[1].insert(x, y, item);
                }
            } else {
                if (x < mBounds.midX) { // bottom left
                    mChildren[2].insert(x, y, item);
                } else {
                    mChildren[3].insert(x, y, item);
                }
            }
            return;
        }
        if (mItems == null) {
            mItems = new ArrayList<T>();
        }
        mItems.add(item);
        if (mItems.size() > MAX_ELEMENTS && mDepth < MAX_DEPTH) {
            split();
        }
    }

    /**
     * Split this quad.
     */
    private void split() {
        mChildren = new PointQuadTree[]{
                new PointQuadTree<T>(mBounds.getTopLeftQuad(), mDepth + 1),
                new PointQuadTree<T>(mBounds.getTopRightQuad(), mDepth + 1),
                new PointQuadTree<T>(mBounds.getBottomLeftQuad(), mDepth + 1),
                new PointQuadTree<T>(mBounds.getBottomRightQuad(), mDepth + 1)
        };

        List<T> items = mItems;
        mItems = null;

        for (T item : items) {
            // re-insert items into child quads.
            insert(item.getPoint().x, item.getPoint().y, item);
        }
    }

    /**
     * Remove the given item from the set.
     *
     * @return whether the item was removed.
     */
    @Override
    public boolean remove(T item) {
        Point point = item.getPoint();
        if (this.mBounds.contains(point.x, point.y)) {
            return remove(point.x, point.y, item);
        } else {
            return false;
        }
    }

    private boolean remove(double x, double y, T item) {
        if (this.mChildren != null) {
            if (y < mBounds.midY) {
                if (x < mBounds.midX) { // top left
                    return mChildren[0].remove(x, y, item);
                } else { // top right
                    return mChildren[1].remove(x, y, item);
                }
            } else {
                if (x < mBounds.midX) { // bottom left
                    return mChildren[2].remove(x, y, item);
                } else {
                    return mChildren[3].remove(x, y, item);
                }
            }
        }
        else {
            return mItems.remove(item);
        }
    }

    /**
     * Removes all points from the quadTree
     */
    @Override
    public void clear() {
        mChildren = null;
        if (mItems != null) {
            mItems.clear();
        }
    }

    /**
     * Search for all items within a given bounds.
     */
    @Override
    public Collection<T> search(Bounds searchBounds) {
        final List<T> results = new ArrayList<T>();
        search(searchBounds, results);
        return results;
    }

    private void search(Bounds searchBounds, Collection<T> results) {
        if (!mBounds.intersects(searchBounds)) {
            return;
        }

        if (this.mChildren != null) {
            for (PointQuadTree<T> quad : mChildren) {
                quad.search(searchBounds, results);
            }
        } else if (mItems != null) {
            if (searchBounds.contains(mBounds)) {
	            results.addAll(mItems);
            } else {
                for (T item : mItems) {
                    if (searchBounds.contains(item.getPoint())) {
                        results.add(item);
                    }
                }
            }
        }
    }
}
