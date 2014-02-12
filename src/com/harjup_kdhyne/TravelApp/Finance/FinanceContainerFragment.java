package com.harjup_kdhyne.TravelApp.Finance;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.harjup_kdhyne.TravelApp.R;

/**
 * Created by Kyle 2.1 on 2/9/14.
 */
public class FinanceContainerFragment extends Fragment
{
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View myView = inflater.inflate(R.layout.finance_activity_container, container, false);
    /*
        ListFragment financePurchaseListFragment = new PurchaseListFragment();
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction.replace(R.id.financeActivityContainer,financePurchaseListFragment);
        transaction.commit();
    */
        Fragment financeSummaryFragment = new SummaryFragment();
        ListFragment financePurchaseListFragment = new PurchaseListFragment();

        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction.replace(R.id.financeSummaryContainer,financeSummaryFragment);
        transaction.replace(R.id.financePurchaseListContainer,financePurchaseListFragment);

        transaction.commit();

        return myView;
    }
}