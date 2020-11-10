/*
 * Copyright (C) 2001 by University of Maryland, College Park, MD 20742, USA
 * and Martin Wattenberg, w@bewitched.com
 * All rights reserved.
 * Authors: Benjamin B. Bederson and Martin Wattenberg
 * http://www.cs.umd.edu/hcil/treemaps
 */

package com.thecloudsite.stockroom.treemap;

import java.util.*;

/*
 *
 * An implementation of MapModel that represents
 * a hierarchical structure. It currently cannot
 * handle structural changes to the tree, since it
 * caches a fair amount of information.
 *
 */
public class TreeModel implements MapModel {
  private final Mappable mapItem;
  private Mappable[] childItems;
  private Mappable[] cachedTreeItems; // we assume tree structure doesn't change.
  private MapModel[] cachedLeafModels;
  private TreeModel parent;
  private final Vector<TreeModel> children = new Vector<>();
  private boolean sumsChildren;

  public TreeModel() {
    this.mapItem = new MapItem();
    sumsChildren = true;
  }

  public TreeModel(Mappable mapItem) {
    this.mapItem = mapItem;
  }

  public void setOrder(int order) {
    mapItem.setOrder(order);
  }

  public MapModel[] getLeafModels() {
      if (cachedLeafModels != null) {
          return cachedLeafModels;
      }
    Vector<TreeModel> v = new Vector<TreeModel>();
    addLeafModels(v);
    int n = v.size();
    MapModel[] m = new MapModel[n];
    v.copyInto(m);
    cachedLeafModels = m;
    return m;
  }

  private void addLeafModels(Vector<TreeModel> v) {
    if (!hasChildren()) {
      return;
    }
      if (!getChild(0).hasChildren()) {
          v.addElement(this);
      } else {
          for (int i = childCount() - 1; i >= 0; i--)
              getChild(i).addLeafModels(v);
      }
  }

  public int depth() {
    if (parent == null) return 0;
    return 1 + parent.depth();
  }

  public void layout(MapLayout tiling) {
    layout(tiling, mapItem.getBounds());
  }

  public void layout(MapLayout tiling, Rect bounds) {
    mapItem.setBounds(bounds);
    if (!hasChildren()) return;
    double s = sum();
    tiling.layout(this, bounds);
    for (int i = childCount() - 1; i >= 0; i--)
      getChild(i).layout(tiling);
  }

  public Mappable[] getTreeItems() {
      if (cachedTreeItems != null) {
          return cachedTreeItems;
      }

    Vector<Mappable> v = new Vector<>();
    addTreeItems(v);
    int n = v.size();
    Mappable[] m = new Mappable[n];
    v.copyInto(m);
    cachedTreeItems = m;
    return m;
  }

  private void addTreeItems(Vector<Mappable> v) {
      if (!hasChildren()) {
          v.addElement(mapItem);
      } else {
          for (int i = childCount() - 1; i >= 0; i--)
              getChild(i).addTreeItems(v);
      }
  }

  private double sum() {
      if (!sumsChildren) {
          return mapItem.getSize();
      }
    double s = 0;
    for (int i = childCount() - 1; i >= 0; i--)
      s += getChild(i).sum();
    mapItem.setSize(s);
    return s;
  }

  public Mappable[] getItems() {
      if (childItems != null) {
          return childItems;
      }
    int n = childCount();
    childItems = new Mappable[n];
    for (int i = 0; i < n; i++) {
      childItems[i] = getChild(i).getMapItem();
      childItems[i].setDepth(1 + depth());
    }
    return childItems;
  }

  public Mappable getMapItem() {
    return mapItem;
  }

  public void addChild(TreeModel child) {
    child.setParent(this);
    children.addElement(child);
    childItems = null;
  }

  public void setParent(TreeModel parent) {
    for (TreeModel p = parent; p != null; p = p.getParent())
      if (p == this) throw new IllegalArgumentException("Circular ancestry!");
    this.parent = parent;
  }

  public TreeModel getParent() {
    return parent;
  }

  public int childCount() {
    return children.size();
  }

  public TreeModel getChild(int n) {
    return children.elementAt(n);
  }

  public boolean hasChildren() {
    return children.size() > 0;
  }
}
