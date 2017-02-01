// Copyright (c) 2017 Art & Logic, Inc. All Rights Reserved.

package com.slaterama.alpc2;

import android.content.Context;
import android.graphics.Point;
import android.graphics.Rect;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @class ACommandParser
 * @brief A utility class that parses a command string and produces formatted
 * output in the form of a series of human-readable commands.
 */
public class ACommandParser {

   private static final int LOW_BOUNDS = -8192;
   private static final int HIGH_BOUNDS = 8191;

   // Used in processing data strings
   private static final int BYTE_LENGTH = 2;
   private static final int COMMAND_CLEAR = 0xF0;
   private static final int COMMAND_PEN_UP_DOWN = 0x80;
   private static final int COMMAND_SET_COLOR = 0xA0;
   private static final int COMMAND_MOVE_PEN = 0xC0;

   // Constants used in creating readable commands
   private static final String SEPARATOR = " ";
   private static final String NEWLINE = "\n";
   private static final String TERMINATOR = ";";
   private static final String OPEN_PAREN = "(";
   private static final String CLOSE_PAREN = ")";
   private static final String COORDINATE_SEPARATOR = ", ";

   // Used in adding additional Pen up/Pen down commands
   private static final List<Integer> PEN_UP = Collections.singletonList(0);
   private static final List<Integer> PEN_DOWN = Collections.singletonList(1);

   /**
    * The bounding rectangle.
    */
   private static final Rect sRect =
       new Rect(LOW_BOUNDS, LOW_BOUNDS, HIGH_BOUNDS, HIGH_BOUNDS);

   /**
    * The {@link Context} used to get resource strings.
    */
   private Context fContext;

   /**
    * The most recent pen state when the pen was last within the bounding
    * rectangle.
    */
   private boolean fLastPenDown;

   /**
    * The current pen state, including when the pen is currently outside
    * the bounding rectangle.
    */
   private boolean fPenDown;

   /**
    * The most recent pen color when the pen was last within the bounding
    * rectangle.
    */
   private Integer[] fLastColor;

   /**
    * The current pen color, including when the pen is currently outside
    * the bounding rectangle.
    */
   private Integer[] fColor;

   /**
    * The current point. This point describes the current location
    * even when the pen has moved outside of the square.
    */
   private Point fCurrentPoint;

   /**
    * Indicates whether a command has just been processed (and therefore
    * we will need to add the command string when we process the next
    * command). This applies only to processing move commands.
    */
   private boolean fBeginNewCommand = true;

   /**
    * Constructor.
    * @param context The {@link Context} to be used for resolving
    *                resource strings.
    */
   public ACommandParser(Context context) {
      super();
      fContext = context;
   }

   /**
    * Helper method to parse a data string.
    * @param data The data string to process.
    * @return The output string.
    */
   public String parse(String data)
       throws ParseException {
      StringBuilder builder = new StringBuilder();
      int start = 0;
      if (TextUtils.isEmpty(data)) {
         throw new ParseException(
             fContext.getString(R.string.error_in_input_data),
             start);
      }

      Integer commandCode = null;
      String hiByte = null;
      List<Integer> parameters = new ArrayList<>();

      // Loop through the data string, extracting one byte (i.e. two
      // characters) at a time.
      try {
         int length = data.length();
         while (start < length) {
            int end = start + BYTE_LENGTH;
            if (end > length) {
               // We encountered a non-even amount of bytes
               throw new ParseException(
                   fContext.getString(R.string.error_in_input_data),
                   start);
            }

            // Extract and decode the current 2-character byte string
            String byteStr = data.substring(start, end);
            int decoded = Integer.decode("0x" + byteStr);

            // Each time we encounter a new command, check to see if we
            // have a prior command waiting to be processed.
            switch (decoded) {
               case COMMAND_CLEAR:
               case COMMAND_PEN_UP_DOWN:
               case COMMAND_SET_COLOR:
               case COMMAND_MOVE_PEN:
                  // Process the previous command
                  if (commandCode != null) {
                     processCommand(builder, commandCode, parameters);
                  }

                  // Save the new command and clear the parameter list
                  commandCode = decoded;
                  parameters = new ArrayList<>();
                  break;
               default:
                  // The decoded byte is not a command, so treat is as
                  // a parameter
                  if (hiByte == null) {
                     hiByte = byteStr;
                  } else {
                     int parameter = AUtils.decode(hiByte, byteStr);
                     parameters.add(parameter);
                     hiByte = null;
                  }
            }
            start = end;
         }

         // Once out of the loop, process the last command
         if (commandCode != null)
            processCommand(builder, commandCode, parameters);
      } catch (IllegalArgumentException e) {
         // NumberFormatException extends IllegalArgumentException so we
         // don't need to explicitly catch it here
         throw new ParseException(
             fContext.getString(R.string.error_in_input_data),
             start);
      }

      return builder.toString();
   }

   /**
    * Processes a command in the form of a command code and a list of
    * integer parameters.
    * @param builder A StringBuilder used for building the final output.
    * @param commandCode The integer code of the command to process.
    * @param parameters A list of integer parameters for the given command.
    */
   private void processCommand(
       @NonNull StringBuilder builder,
       int commandCode,
       @NonNull List<Integer> parameters) {
      processCommand(builder, commandCode, parameters, false);
   }

   /**
    * Processes a command in the form of a command code and a list of
    * integer parameters.
    * @param builder A StringBuilder used for building the final output.
    * @param commandCode The integer code of the command to process.
    * @param parameters A list of integer parameters for the given command.
    * @param alwaysOutput Whether to add to the list of commands regardless of
    *                     whether the current pointer is within the square or
    *                     not. Normally, Pen up/pen down and color commands
    *                     are ignored (i.e. not output) when the pointer is
    *                     outside the square. However, there are times when we
    *                     are moving from inside the square to outside
    *                     (or vice versa) when we need to output a command
    *                     regardless of the current location.
    */
   private void processCommand(
       @NonNull StringBuilder builder,
       int commandCode,
       @NonNull List<Integer> parameters,
       boolean alwaysOutput) throws IllegalArgumentException {
      switch (commandCode) {
         case COMMAND_CLEAR:
            // Clear command expects zero parameters
            if (parameters.size() != 0) {
               throw new IllegalArgumentException();
            }
            processClear(builder);
            break;
         case COMMAND_PEN_UP_DOWN:
            // Pen up/pen down command expects one parameter
            if (parameters.size() != 1) {
               throw new IllegalArgumentException();
            }
            processPen(
                builder,
                (parameters.get(0) != 0),
                alwaysOutput);
            break;
         case COMMAND_SET_COLOR:
            // Set color command expects four parameters
            if (parameters.size() != 4) {
               throw new IllegalArgumentException();
            }
            processColor(
                builder,
                parameters.get(0),
                parameters.get(1),
                parameters.get(2),
                parameters.get(3),
                alwaysOutput);
            break;
         case COMMAND_MOVE_PEN:
            // Move pen command expects a non-zero, even number of parameters
            int size = parameters.size();
            if (size == 0 || (size & 1) == 1) {
               throw new IllegalArgumentException();
            }
            processMove(builder, parameters);
            break;
         default:
            // Unknown command
            throw new IllegalArgumentException();
      }
      fBeginNewCommand = true;
   }

   /**
    * Processes a "Clear" command.
    * @param builder A StringBuilder used for building the final output.
    */
   private void processClear(@NonNull StringBuilder builder) {
      // Reset all saved values
      fCurrentPoint = new Point(0, 0);
      fLastPenDown = false;
      fPenDown = false;
      fLastColor = new Integer[] {0, 0, 0, 0};
      fColor = new Integer[] {0, 0, 0, 0};

      // Append the new command
      if (builder.length() > 0) {
         builder.append(NEWLINE);
      }
      builder.append(fContext.getString(R.string.command_clear))
          .append(TERMINATOR);
   }

   /**
    * Processes a "Pen" command.
    * @param builder A StringBuilder used for building the final output.
    * @param penDown Whether or not the pen is down.
    * @param alwaysOutput Whether to output this command regardless of
    *                     whether or not we are currently inside the
    *                     bounding rectangle.
    */
   private void processPen(
       @NonNull StringBuilder builder,
       boolean penDown,
       boolean alwaysOutput) {
      // Save the current pen state
      fPenDown = penDown;
      boolean contains = RectUtils.contains(sRect, fCurrentPoint);

      // If we're within the bounding rectangle, update fLastPenDown
      if (contains) {
         fLastPenDown = fPenDown;
      }

      if (contains || alwaysOutput) {
         // First, check if we need to process a color change
         if (!Arrays.equals(fLastColor, fColor)) {
            fLastColor = Arrays.copyOf(fColor, fColor.length);
            processCommand(
                builder,
                COMMAND_SET_COLOR,
                Arrays.asList(fLastColor),
                true);
         }

         // Append the new command
         if (builder.length() > 0) {
            builder.append(NEWLINE);
         }
         builder.append(fContext.getString(R.string.command_pen_up_down))
             .append(SEPARATOR)
             .append(fPenDown
                 ? fContext.getString(R.string.command_pen_down)
                 : fContext.getString(R.string.command_pen_up))
             .append(TERMINATOR);
      }
   }

   /**
    * Processes a "Color" command.
    * @param builder A StringBuilder used for building the final output.
    * @param red The red value.
    * @param green The green value.
    * @param blue The blue value.
    * @param alpha The alpha value.
    * @param alwaysOutput Whether to output this command regardless of
    *                     whether or not we are currently inside the
    *                     bounding rectangle.
    */
   private void processColor(
      @NonNull StringBuilder builder,
      int red,
      int green,
      int blue,
      int alpha,
      boolean alwaysOutput) {
      fColor = new Integer[] { red, green, blue, alpha };
      boolean contains = RectUtils.contains(sRect, fCurrentPoint);
      if (contains)
         fLastColor = Arrays.copyOf(fColor, fColor.length);
      if (contains || alwaysOutput) {
         if (builder.length() > 0) {
            builder.append(NEWLINE);
         }
         builder.append(fContext.getString(R.string.command_set_color))
             .append(SEPARATOR)
             .append(red)
             .append(SEPARATOR)
             .append(green)
             .append(SEPARATOR)
             .append(blue)
             .append(SEPARATOR)
             .append(alpha)
             .append(TERMINATOR);
      }
   }

   /**
    * Processes a "Move" command.
    * @param builder A StringBuilder used for building the final output.
    * @param parameters A list of integer values describing the move.
    */
   private void processMove(
       @NonNull StringBuilder builder,
       List<Integer> parameters) {
      boolean needsTerminator = false;

      // Loop through the parameters, taking two at a time as x- and y-offsets
      int size = parameters.size();
      for (int i = 0; i < size; i += 2) {
         final boolean isLastCoordinate = (i + 2 >= size);
         final Point startPoint = new Point(fCurrentPoint);
         fCurrentPoint.offset(parameters.get(i), parameters.get(i + 1));

         // If pen is up and we haven't hit the last move command yet, we can
         // just continue here as we don't need to record this.
         if (!fPenDown && !isLastCoordinate) {
            continue;
         }

         // Create a line containing the start point and current point
         ALine line = new ALine(startPoint, fCurrentPoint);

         // Intersect the line with the bounding rectangle to see if any
         // part of the line is outside the rectangle.
         ALine adjusted = ALine.intersection(line, sRect);

         // If the intersected line is null, the line never crossed the
         // bounding rectangle, so no action is needed.
         if (adjusted == null) {
            continue;
         }

         // Determine which of the two points of the adjusted line, if any,
         // are outside of the bounding rectangle
         boolean point1InBounds =
             line.getPoint1().equals(adjusted.getPoint1());
         boolean point2InBounds = needsTerminator =
             line.getPoint2().equals(adjusted.getPoint2());

         if (point1InBounds && point2InBounds) {
            // Both points are in bounds, so we can simply add to the
            // current "Move" command string.
            addPoint(builder, line.getPoint2());
         } else {
            if (!point1InBounds) {
               // We are moving FROM outside of the bounding rectangle.
               // In this case, move to the adjusted point 1.
               addPoint(builder, adjusted.getPoint1());

               // We've now entered the square. If the pen was down when we
               // left, OR it was set down while we were outside the square,
               // we have to ensure it is down now.
               if (fLastPenDown || fPenDown) {
                  builder.append(TERMINATOR);

                  // Process a "Pen down" command
                  processCommand(
                      builder,
                      COMMAND_PEN_UP_DOWN,
                      PEN_DOWN,
                      true);
               }
            }

            // Now we can add the "adjusted" point 2.
            addPoint(builder, adjusted.getPoint2());

            if (!point2InBounds) {
               // We are moving TO outside of the bounding rectangle.
               // In this case, we have to pick up the pen.
               builder.append(TERMINATOR);

               // Process a "Pen up" command
               processCommand(
                   builder,
                   COMMAND_PEN_UP_DOWN,
                   PEN_UP,
                   true);
            }
         }
      }
      if (needsTerminator) {
         builder.append(TERMINATOR);
      }
   }

   /**
    * Append a point encountered during a move command.
    * @param builder A StringBuilder used for building the final output.
    * @param point The point to append to the builder.
    */
   private void addPoint(
       StringBuilder builder,
       Point point) {
      // Append the command string itself if necessary
      if (fBeginNewCommand) {
         if (builder.length() > 0) {
            builder.append(NEWLINE);
         }
         builder.append(fContext.getString(R.string.command_move_pen));
         fBeginNewCommand = false;
      }

      // Append the point
      builder.append(SEPARATOR)
          .append(OPEN_PAREN)
          .append(point.x)
          .append(COORDINATE_SEPARATOR)
          .append(point.y)
          .append(CLOSE_PAREN);
   }
}
