/*
 * Copyright (C) 2001 by University of Maryland, College Park, MD 20742, USA
 * and Martin Wattenberg, w@bewitched.com
 * All rights reserved.
 * Authors: Benjamin B. Bederson and Martin Wattenberg
 * http://www.cs.umd.edu/hcil/treemaps
 */

package com.thecloudsite.stockroom.treemap;

/*
 *
 * Interface representing an object that can be placed
 * in a treemap layout.
 * <p>
 * The properties are:
 * <ul>
 * <li> size: corresponds to area in map.</li>
 * <li> order: the sort order of the item. </li>
 * <li> depth: the depth in hierarchy. </li>
 * <li> bounds: the bounding rectangle of the item in the map.</li>
 * </ul>
 *
 */
public interface Mappable {
  double getSize();

  void setSize(double size);

  Rect getBounds();

  void setBounds(Rect bounds);

  void setBounds(double x, double y, double w, double h);

  int getOrder();

  void setOrder(int order);

  int getDepth();

  void setDepth(int depth);
}
