// Copyright (c) 2017 Art & Logic, Inc. All Rights Reserved.

package com.slaterama.alpc2;

import android.graphics.Point;
import android.graphics.Rect;
import android.support.annotation.NonNull;

import java.util.Locale;

/**
 * @class ALine
 * @brief A class that represents a line segment.
 */
public class ALine {

   /**
    * Returns the intersection of a line and a rectangle.
    * @param line The line being intersected.
    * @param rect The rectangle being intersected.
    * @return The resulting line that represents the intersection of a line
    * and a rectangle, or null if the line and rectangle do not intersect.
    * (If the line is fully inside the rectangle, return a copy of that line.)
    */
   public static ALine intersection(@NonNull ALine line, @NonNull Rect rect) {
      // Does the rectangle contain either (or both) of the line's points?
      boolean rectContainsPoint1 =
          RectUtils.contains(rect, line.fPoint1);
      boolean rectContainsPoint2 =
          RectUtils.contains(rect, line.fPoint2);

      // If the rectangle contains
      // both points, just return a copy of the line
      if (rectContainsPoint1 && rectContainsPoint2) {
         return new ALine(line);
      }

      // A line can only intersect a rectangle in at most two points. To get
      // the intersection, we'll convert the rectangle's four sides into
      // lines.
      Point point1 = null;
      Point point2 = null;
      ALine[] sides = {
          new ALine(rect.left, rect.bottom, rect.left, rect.top),
          new ALine(rect.left, rect.top, rect.right, rect.top),
          new ALine(rect.right, rect.top, rect.right, rect.bottom),
          new ALine(rect.right, rect.bottom, rect.left, rect.bottom)
      };

      // Test each of the rectangle's sides for an intersection with the
      // supplied line. If we've hit two intersection points, we can break
      // out of the loop.
      for (ALine side : sides) {
         Point intersection = intersection(line, side);
         if (intersection != null) {
            if (point1 == null) {
               point1 = intersection;
            } else {
               point2 = intersection;
               break;
            }
         }
      }

      // If we got at least one intersection point, and either of the line's
      // endpoints were inside the rectangle, return a new line based on those
      // two points
      if (point1 != null) {
         if (rectContainsPoint1) {
            // The line crossed the rectangle in one point, and the line's point1
            // was inside the rectangle. Se return a new line consisting of
            // the line's point1 and this single intersection point.
            return new ALine(line.fPoint1, point1);
         } else if (rectContainsPoint2) {
            // The line crossed the rectangle in one point, and the line's point2
            // was inside the rectangle. Se return a new line consisting of
            // the line's point2 and this single intersection point.
            return new ALine(point1, line.fPoint2);
         }
      }

      if (point2 != null) {
         // The line crossed the rectangle in two points. Return a new line
         // consisting of those two points.
         return new ALine(point1, point2);
      }

      // The line never crossed the rectangle, so return null.
      return null;
   }

   /**
    * Calculates the intersection of two lines. NOTE: For the purposes of
    * this application, lines that are parallel will be considered as NOT
    * intersecting, even if they touch. This method uses the line function
    * y = mx + b, where m is the slope and b is the y-intercept.
    * @param line1 The first line being intersected.
    * @param line2 The second line being intersected.
    * @return The point where the two lines intersect, or null if the
    * two lines are parallel or do not intersect.
    */
   public static Point intersection(
       @NonNull ALine line1,
       @NonNull ALine line2) {
      boolean v1 = line1.isVertical();
      boolean v2 = line2.isVertical();
      if (v1 && v2) {
         // Both lines are vertical
         return null;
      }

      Point point;
      if (v1) {
         // Line 1 is vertical
         point = intersection(line2, line1.getPoint1().x);
      } else if (v2) {
         // Line 2 is vertical
         point = intersection(line1, line2.getPoint1().x);
      } else {
         // Neither line is vertical
         float m1 = line1.slope();
         float m2 = line2.slope();
         if (Float.compare(m1, m2) == 0) {
            // Lines are parallel
            return null;
         }

         // Get the y-intercepts
         float b1 = line1.yIntercept();
         float b2 = line2.yIntercept();

         // We can now solve for x using formula for slope
         float x = (b2 - b1) / (m1 - m2);
         float y = m1 * x + b1;
         point = new Point(Math.round(x), Math.round(y));
      }

      // Only return this point if both lines contain it
      if (point != null
          && isInBounds(line1, point)
          && isInBounds(line2, point)) {
         return point;
      } else {
         return null;
      }
   }

   /**
    * Calculates the intersection of a non-vertical line with a vertical line
    * (represented by an x-value).
    * @param nonVerticalLine A non-vertical line being intersected.
    * @param x The x-value to use to find the intersection point on the
    *          non-vertical line.
    * @return The point where the non-vertical line is crossed at the given
    * x-value, or null if the x-value does not cross the line.
    */
   private static Point intersection(
       @NonNull ALine nonVerticalLine,
       int x) {
      if (nonVerticalLine.isVertical()) {
         throw new IllegalArgumentException(
             "nonVerticalLine cannot be vertical");
      }

      final float m2 = nonVerticalLine.slope();
      final float b2 = nonVerticalLine.yIntercept();
      final int y = Math.round(m2 * x + b2);

      Point point = new Point(x, y);
      if (isInBounds(nonVerticalLine, point)) {
         return point;
      } else {
         return null;
      }
   }

   /**
    * Returns whether the given point is within the bounds defined by this line.
    * @param point The point to test.
    * @return Whether the given point is within the bounds defined by this line.
    */
   private static boolean isInBounds(
       @NonNull ALine line,
       @NonNull Point point) {
      return (point.x >= Math.min(line.fPoint1.x, line.fPoint2.x)
          && point.x <= Math.max(line.fPoint1.x, line.fPoint2.x)
          && point.y >= Math.min(line.fPoint1.y, line.fPoint2.y)
          && point.y <= Math.max(line.fPoint1.y, line.fPoint2.y));
   }

   /**
    * The first point that defines this line.
    */
   private Point fPoint1;

   /**
    * The second point that defines this line.
    */
   private Point fPoint2;

   /**
    * Constructor that takes an existing line as an argument.
    * @param line The line to use to construct this line.
    */
   public ALine(@NonNull ALine line) {
      fPoint1 = new Point(line.fPoint1);
      fPoint2 = new Point(line.fPoint2);
   }

   /**
    * Constructor that takes two points as arguments.
    * @param point1 The first point to use to construct this line.
    * @param point2 The second point to use to construct this line.
    */
   public ALine(@NonNull Point point1, @NonNull Point point2) {
      fPoint1 = new Point(point1);
      fPoint2 = new Point(point2);
   }

   /**
    * Constructor that takes four coordinate values as arguments.
    * @param x1 The first x-coordinate to use to construct this line.
    * @param y1 The first y-coordinate to use to construct this line.
    * @param x2 The second x-coordinate to use to construct this line.
    * @param y2 The second y-coordinate to use to construct this line.
    */
   public ALine(int x1, int y1, int x2, int y2) {
      fPoint1 = new Point(x1, y1);
      fPoint2 = new Point(x2, y2);
   }

   /**
    * Returns the first point.
    * @return The first point.
    */
   public Point getPoint1() {
      return fPoint1;
   }

   /**
    * Returns the second point.
    * @return The second point.
    */
   public Point getPoint2() {
      return fPoint2;
   }

   /**
    * Returns whether this line is vertical, i.e. fPoint1.x == fPoint2.x.
    * @return Whether this line is vertical.
    */
   public boolean isVertical() {
      return (fPoint1.x == fPoint2.x);
   }

   /**
    * Returns the slope of this line. If this line is vertical, this method
    * will return {@link Float#POSITIVE_INFINITY}.
    * @return The slope of this line.
    */
   public float slope() {
      return (isVertical() ?
          Float.POSITIVE_INFINITY :
          (float) (fPoint2.y - fPoint1.y) / (float) (fPoint2.x - fPoint1.x));
   }

   /**
    * Returns the y-intercept of this line, i.e. the y-value of this line where
    * it crosses the y axis. If this line is vertical, this method will
    * return {@link Float#NaN} (even when the x-value is zero).
    * @return The y-intercept of this line.
    */
   public float yIntercept() {
      return (isVertical() ?
          Float.NaN :
          fPoint1.y - slope() * fPoint1.x);
   }

   @Override
   public String toString() {
      return String.format(
          Locale.US,
          "ALine((%d, %d), (%d, %d))",
          fPoint1.x,
          fPoint1.y,
          fPoint2.x,
          fPoint2.y);
   }
}
