// Copyright (c) 2017 Art & Logic, Inc. All Rights Reserved.

package com.slaterama.alpc2;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.text.ParseException;

/**
 * @class AMainActivity
 * @brief The main activity for the ALPC2 application. Consists of an input
 * field for entering a command string, as well as an option to select from
 * a list of pre-configured sample data (i.e. Simple Green Line, Blue Square,
 * etc.)
 */
public class AMainActivity extends AppCompatActivity
    implements DialogInterface.OnClickListener {

   private final static String TAG_DIALOG_FRAGMENT = "dialogFragment";

   // Used for preserving state of fOutputText
   private static final String KEY_OUTPUT_TEXT = "outputText";
   private static final String KEY_OUTPUT_TEXT_COLOR = "outputTextColor";

   /**
    * A string array that holds the "example" string inputs used by
    * the sample data dialog fragment.
    */
   private static String[] sInputData;

   /**
    * The sample data dialog fragment.
    */
   private SampleDataDialogFragment fSampleDataDialogFragment;

   /**
    * Gets the corresponding sample data string based on a given index.
    * @param context The context to use to retrieve the data string.
    * @param index The index of the desired data string.
    * @return The corresponding data string.
    */
   private static String getDataString(Context context, int index) {
      if (sInputData == null)
         sInputData = context.getResources().getStringArray(R.array.input_data);
      try {
         return sInputData[index];
      } catch (IndexOutOfBoundsException e) {
         return null;
      }
   }

   /**
    * The EditText for entering a command string.
    */
   private EditText fCommandStringEdit;

   /**
    * The TextView that holds the output.
    */
   private TextView fOutputText;

   /**
    * A helper class that parses the command string.
    */
   private ACommandParser fCommandParser;

   @Override
   protected void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.activity_main);
      fCommandStringEdit =
          (EditText) findViewById(R.id.activity_main_command_string);
      fOutputText = (TextView) findViewById(R.id.output);

      // State of TextViews is not preserved on configuration change
      if (savedInstanceState != null) {
         fOutputText.setText(
             savedInstanceState.getCharSequence(KEY_OUTPUT_TEXT));
         fOutputText.setTextColor(
             savedInstanceState.getInt(KEY_OUTPUT_TEXT_COLOR));
      }

      // Configure datalog fragment if visible
      fSampleDataDialogFragment =
          (SampleDataDialogFragment) getSupportFragmentManager()
              .findFragmentByTag(TAG_DIALOG_FRAGMENT);
      if (fSampleDataDialogFragment != null)
         fSampleDataDialogFragment.setOnClickListener(this);
   }

   @Override
   public void onSaveInstanceState(Bundle outState) {
      super.onSaveInstanceState(outState);

      // State of TextViews is not preserved on configuration change
      outState.putCharSequence(KEY_OUTPUT_TEXT, fOutputText.getText());
      outState.putInt(KEY_OUTPUT_TEXT_COLOR, fOutputText.getCurrentTextColor());
   }

   /**
    * Handles a click from the sample data dialog.
    * @param dialog The sample data dialog.
    * @param which Which button was clicked.
    */
   @Override
   public void onClick(DialogInterface dialog, int which) {
      String inputData = getDataString(this, which);
      if (TextUtils.isEmpty(inputData))
         Toast.makeText(
             this,
             R.string.an_error_occurred,
             Toast.LENGTH_LONG)
             .show();
      else {
         fCommandStringEdit.setText(inputData);
         fOutputText.setText("");
         fOutputText.setTextColor(Color.BLACK);
      }
   }

   /**
    * Convenience method to hide the soft keyboard.
    */
   private void hideSoftKeyboard() {
      View view = getCurrentFocus();
      if (view != null) {
         InputMethodManager manager =
             (InputMethodManager) getSystemService(
                 Context.INPUT_METHOD_SERVICE);
         manager.hideSoftInputFromWindow(view.getWindowToken(), 0);
      }
   }

   /**
    * Called when the "Select from sample data" button is clicked. Shows the
    * sample data dialog.
    * @param view The "Select from sample data" button
    */
   public void onSelectClick(View view) {
      if (fSampleDataDialogFragment == null) {
         fSampleDataDialogFragment = new SampleDataDialogFragment();
         fSampleDataDialogFragment.setOnClickListener(this);
      }
      fSampleDataDialogFragment.show(
          getSupportFragmentManager(),
          TAG_DIALOG_FRAGMENT);
   }

   /**
    * Called when the "Process" button is clicked. Parses the command string
    * and displays the result in fOutputText.
    * @param view The "Process" button
    */
   public void onProcessClick(View view) {
      hideSoftKeyboard();
      if (fCommandParser == null) {
         fCommandParser = new ACommandParser(this);
      }
      try {
         String output = fCommandParser.parse(
             fCommandStringEdit.getText().toString());
         fOutputText.setText(output);
         fOutputText.setTextColor(Color.BLACK);
      } catch (ParseException e) {
         fOutputText.setText(e.getMessage());
         fOutputText.setTextColor(Color.RED);
      }
   }

   /**
    * A dialog that shows sample data to chose from.
    */
   public static class SampleDataDialogFragment extends DialogFragment {

      private DialogInterface.OnClickListener mOnClickListener;

      @NonNull
      @Override
      public Dialog onCreateDialog(Bundle savedInstanceState) {
         AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
             .setTitle(R.string.select_from_sample_data)
             .setItems(R.array.input_data_labels, mOnClickListener);
         return builder.create();
      }

      public void setOnClickListener(DialogInterface.OnClickListener listener) {
         mOnClickListener = listener;
      }
   }
}
