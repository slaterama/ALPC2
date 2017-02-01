// Copyright (c) 2017 Art & Logic, Inc. All Rights Reserved.

package com.slaterama.alpc2;

/**
 * @class AUtils
 * @brief Contains methods for encoding and decoding values. NOTE: This is
 * the same class that was submitted for ALPC1, which is why it contains
 * the "encode" method although it is not actually being used in this app.
 */
public class AUtils {

   // Minimum/maximum allowed input values
   private static final int ENCODE_MIN_VALUE = -8192;
   private static final int ENCODE_MAX_VALUE = 8191;
   private static final int DECODE_MIN_VALUE = 0;
   private static final int DECODE_MAX_VALUE = 0x7F;

   // Amount to translate input by for encoding/decoding
   private static final int TRANSLATION = 8192;

   // Encoding masks
   private static final int ENCODE_LOW_ORDER_MASK = 0x7F;
   private static final int ENCODE_HIGH_ORDER_MASK = 0xFF80;

   // Used to ensure correct hexadecimal format for decoding
   private static final String DECODE_REGEX = "^0x";
   private static final String DECODE_FORMAT = "0x%s";

   /**
    * Encodes an integer value to a four-digit hexadecimal value.
    * @param value The integer value to encode.
    * @return The encoded four-digit hexadecmial string.
    */
   public static String encode(int value) {
      if (value < ENCODE_MIN_VALUE || value > ENCODE_MAX_VALUE)
         throw new IllegalArgumentException();

      int intermediateValue = value + TRANSLATION;
      int lowOrderValue = intermediateValue & ENCODE_LOW_ORDER_MASK;
      int highOrderValue = intermediateValue & ENCODE_HIGH_ORDER_MASK;
      int encodedValue = highOrderValue << 1 | lowOrderValue;

      return String.format("%04X", encodedValue);
   }

   /**
    * Decodes a pair of two-digit hexadecimal strings into an integer value.
    * @param hiByte The high-order hexadecimal string in the hi/lo pair.
    * @param loByte The low-order hexadecimal string in the hi/lo pair.
    * @return The decoded integer value.
    */
   public static int decode(String hiByte, String loByte) {
      try {
         if (!hiByte.matches(DECODE_REGEX))
            hiByte = String.format(DECODE_FORMAT, hiByte);
         if (!loByte.matches(DECODE_REGEX))
            loByte = String.format(DECODE_FORMAT, loByte);
         int hiInt = Integer.decode(hiByte);
         int loInt = Integer.decode(loByte);
         if (hiInt < DECODE_MIN_VALUE || hiInt > DECODE_MAX_VALUE
             || loInt < DECODE_MIN_VALUE || loInt > DECODE_MAX_VALUE)
            throw new IllegalArgumentException();
         return (hiInt << 7 | loInt) - TRANSLATION;
      } catch (NumberFormatException e) {
         throw new IllegalArgumentException();
      }
   }

   /**
    * Private constructor to prevent instantiation.
    */
   private AUtils() {
   }
}
