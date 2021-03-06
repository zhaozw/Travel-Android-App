package com.harjup_kdhyne.TravelApp.Finance;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import com.harjup_kdhyne.TravelApp.R;
import org.joda.time.DateTime;
import org.joda.time.Period;
import org.openexchangerates.oerjava.Currency;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.Date;

/**
 * Created by Kyle 2.1 on 2/16/14.
 * Controls the trip settings fragment, where a user can change the travel dates and maximum budget
 */
public class TripSettingsEditFragment extends Fragment
{
    private FinanceDataSource dataSource;
    private TripSettings currentTrip;

    //Keys for ID and start/end date
    public static final String TRIP_SERIALIZABLE_ID = "com.harjup_kdhyne.TravelApp.TripSettings.TRIP";
    public static final String START_DATE = "com.harjup_kdhyne.TravelApp.Finance.start_date";
    public static final String END_DATE = "com.harjup_kdhyne.TravelApp.Finance.end_date";

    //Used to track when we are performing an action on start/end date
    public static final int REQUEST_START_DATE = 0;
    public static final int REQUEST_END_DATE = 1;

    private EditText startDateEditText;
    private EditText endDateEditText;

    private TextView currentExchangeTextView;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        Bundle bundle = getArguments();
        if (bundle != null)
        {
            currentTrip = (TripSettings) bundle.getSerializable(TRIP_SERIALIZABLE_ID);
        }

        super.onCreate(savedInstanceState);
    }

    void openDbConnection()
    {
        dataSource = new FinanceDataSource(getActivity());

        try
        {
            dataSource.open();
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        openDbConnection();

        View myView = inflater.inflate(R.layout.finance_trip_settings_edit, container, false);

        if (myView != null)
        {
            //Retrieve EditTexts
            startDateEditText = (EditText)myView.findViewById(R.id.startDateEditText);
            endDateEditText = (EditText)myView.findViewById(R.id.endDateEditText);
            EditText totalBudgetEditText = (EditText)myView.findViewById(R.id.totalBudgetEditText);

            //Fill editTexts if they aren't null
            if (startDateEditText != null)
                startDateEditText.setText(currentTrip.getStartDateAsString());

            if (endDateEditText != null)
                endDateEditText.setText(currentTrip.getEndDateAsString());

            if (totalBudgetEditText != null)
            {
                totalBudgetEditText.setText(String.valueOf(currentTrip.getTotalBudget()));
                totalBudgetEditText.addTextChangedListener(getTextWatcher());
            }

            currentExchangeTextView = (TextView)myView.findViewById(R.id.currentExchangeTextView);

            if (currentExchangeTextView != null)
            {
                //Get current exchange rate if timeStamp of last exchange is outdated
                if (isExchangeOutdated(currentTrip.getExchangeRateTimeStamp()))
                {
                    new AsyncGetExchangeRate(currentTrip.getTargetCurrency())
                    {
                        @Override
                        protected void onPostExecute(BigDecimal result)
                        {
                            currentExchangeTextView.setText("1 USD = " + result.toString() + " " + currentTrip.getTargetCurrency());
                            currentTrip.setCurrentExchangeRate(Double.parseDouble(result.toString()));
                        }
                    }.execute();
                }

                currentExchangeTextView.setText(String.valueOf(currentTrip.getCurrentExchangeRate()));
            }

            //Retrieve buttons
            Button backButton = (Button) myView.findViewById(R.id.backButton);
            Button saveButton = (Button)myView.findViewById(R.id.saveButton);
            Button updateExchangeButton = (Button)myView.findViewById(R.id.updateExchangeButton);

            backButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v)
                {
                    //Return without saving
                    returnToSummary();
                }
            });

            saveButton.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    //Save or update TripSettings, then return to Summary
                    if (currentTrip != null)
                    {
                        if (currentTrip.getStartDate().isBefore(currentTrip.getEndDate()))
                        {
                            Log.d("Trip is here", String.valueOf(currentTrip.getTripID()));
                            //Check to see if the ID hasn't been set yet (isn't in the list)
                            //Else update the changes
                            if (currentTrip.getTripID() == -1)
                                dataSource.createTripSettings(currentTrip);
                            else
                                dataSource.updateTripSettings(currentTrip);

                            //Return to financeSummary after saving
                            returnToSummary();
                        }
                        //else
                        //{
                            //TODO:Make message box to inform user of date error
                        //}
                    }
                }
            });

            updateExchangeButton.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    new AsyncGetExchangeRate(currentTrip.getTargetCurrency())
                    {
                        @Override
                        protected void onPostExecute(BigDecimal result)
                        {
                            currentExchangeTextView.setText("1 USD = " + result.toString() + " " + currentTrip.getTargetCurrency());
                            currentTrip.setCurrentExchangeRate(Double.parseDouble(result.toString()));
                        }
                    }.execute();
                }
            });

            //Setup OnClickListener for the date textBox and prompt a date picker
            startDateEditText.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    FragmentManager fragManager = getFragmentManager();
                    DatePickerDialogFragment dateDialog = DatePickerDialogFragment.newInstance(currentTrip.getStartDate().toDate());
                    dateDialog.setTargetFragment(TripSettingsEditFragment.this, REQUEST_START_DATE);
                    dateDialog.show(fragManager, START_DATE);
                }
            });

            //Setup OnClickListener for the date textBox and prompt a date picker
            endDateEditText.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    FragmentManager fragManager = getFragmentManager();
                    DatePickerDialogFragment dateDialog = DatePickerDialogFragment.newInstance(currentTrip.getEndDate().toDate());
                    dateDialog.setTargetFragment(TripSettingsEditFragment.this, REQUEST_END_DATE);
                    dateDialog.show(fragManager, END_DATE);
                }
            });

            //Spinner to select currency used to purchase the item
            Spinner purchaseCurrencySpinner = (Spinner)myView.findViewById(R.id.targetCurrencySpinner);

            if (purchaseCurrencySpinner != null)
            {
                ArrayAdapter<Currency> adapter = new ArrayAdapter<Currency>(this.getActivity(), android.R.layout.simple_spinner_item, Currency.values());

                //Populate the spinner with the Currency enum
                purchaseCurrencySpinner.setAdapter(adapter);

                purchaseCurrencySpinner.setSelection(adapter.getPosition(currentTrip.getTargetCurrency()));

                purchaseCurrencySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                        Currency paidCurrency = (Currency) parent.getItemAtPosition(position);
                        if (paidCurrency != null) {
                            Log.d("Currency", paidCurrency.toString());
                        }
                        currentTrip.setTargetCurrency(paidCurrency);
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {

                    }
                });
            }
        }

        return myView;
    }

    /**
     * Called when the OK button is clicked. Need this for the date picker dialog
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        // If this wasn't called by the date dialog exit
        if(resultCode != Activity.RESULT_OK)
            return;

        if(requestCode == REQUEST_START_DATE)
        {
            Date date = (Date) data.getSerializableExtra(DatePickerDialogFragment.DATE);
            currentTrip.setStartDate(new DateTime(date));
            startDateEditText.setText(currentTrip.getStartDateAsString());
        }
        else if(requestCode == REQUEST_END_DATE)
        {
            Date date = (Date) data.getSerializableExtra(DatePickerDialogFragment.DATE);
            currentTrip.setEndDate(new DateTime(date));
            endDateEditText.setText(currentTrip.getEndDateAsString());
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState)
    {
        outState.putSerializable(TRIP_SERIALIZABLE_ID, currentTrip);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onDestroyView()
    {
        try { dataSource.close();}
        catch (Exception e) { e.printStackTrace(); }

        super.onDestroyView();
    }

    private  TextWatcher getTextWatcher()
    {
        return new TextWatcher()
        {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after)
            {
                //Auto-generated method stub
            }

            @Override
            public void onTextChanged(CharSequence arg0, int arg1, int arg2, int arg3)
            {
                Log.d("TEXT WAS CHANGED", arg0.toString());
                try
                {
                    currentTrip.setTotalBudget(Double.parseDouble(arg0.toString()));
                }
                catch (Exception e)
                {
                    Log.e("Null double", arg0.toString());
                }
            }

            @Override
            public void afterTextChanged(Editable s)
            {
                //Auto-generated method stub
            }
        };
    }

    /**
     * Checks to see if the exchange rate is outdated given the tripSetting's refresh frequency
     * @param lastTimeStamp the timeStamp of the latest exchangeRate update
     * @return whether or not the exchangeRate is outdated
     */
    public boolean isExchangeOutdated(DateTime lastTimeStamp)
    {
        Period refreshPeriod = new Period();
        TripSettings.FrequencySettings updateFrequency = currentTrip.getRefreshFrequency();
        switch (updateFrequency)
        {
            case THREE_DAYS:
                refreshPeriod.withDays(3);
                break;
            case EVERY_OTHER:
                refreshPeriod.withDays(2);
                break;
            case DAILY:
                refreshPeriod.withDays(1);
                break;
            case HOURLY:
                refreshPeriod.withHours(1);
                break;
            default:
                return false;
        }

        return lastTimeStamp.plus(refreshPeriod).isBeforeNow();
    }

    /**
     * Return to the trip summary
     */
    public void returnToSummary()
    {
        SummaryFragment summaryFragment = new SummaryFragment();

        Bundle args = new Bundle();
        args.putSerializable(TRIP_SERIALIZABLE_ID, currentTrip);
        summaryFragment.setArguments(args);

        FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
        fragmentTransaction.replace(R.id.financeSummaryContainer, summaryFragment);
        fragmentTransaction.commit();
    }
}

