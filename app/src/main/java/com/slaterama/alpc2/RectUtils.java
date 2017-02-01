// Copyright (c) 2017 Art & Logic, Inc. All Rights Reserved.

package com.slaterama.alpc2;

import android.graphics.Point;
import android.graphics.Rect;
import android.support.annotation.NonNull;

/**
 * @class RectUtils
 * @brief This is a utility class with a single method,
 * {@link RectUtils#contains(Rect, Point)}. The reason for this method is
 * because Java's default {@link Rect#contains(int, int)} method does NOT
 * consider the right and bottom border to be inside the rectangle. For our
 * purposes we want the right and bottom to also be considered inside the
 * rectangle so we'll use the contains method from this class instead of
 * the implementation found in Rect.
 */
public class RectUtils {
   /**
    * A convenience method similar to {@link Rect#contains(int, int)} except
    * that the right and bottom of the rectangle are also considered to be
    * inside.
    * @param rect The rectangle being tested for containment
    * @param point The point being tested for containment
    * @return true iff the point (x, y) is contained by the rectangle,
    *              where containment means left <= x <= right and
    *              top <= y <= bottom
    */
   public static boolean contains(@NonNull Rect rect, @NonNull Point point) {
      return rect.left < rect.right
          && rect.top < rect.bottom
          && point.x >= rect.left
          && point.x <= rect.right
          && point.y >= rect.top
          && point.y <= rect.bottom;
   }

   /**
    * Private constructor to prevent instantiation.
    */
   private RectUtils() {
   }
}
