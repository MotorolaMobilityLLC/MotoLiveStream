package com.motorola.livestream.ui;

import android.app.Dialog;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;

import com.motorola.livestream.R;


public class CustomDialogFragment extends DialogFragment {

    int mBtnStrId1;
    int mBtnStrId2;
    View.OnClickListener mListener1;
    View.OnClickListener mListener2;
    int mMessageId;

    CustomDialogFragment(int messageId, int strId1, View.OnClickListener l1, int strId2, View.OnClickListener l2) {
        mBtnStrId1 = strId1;
        mBtnStrId2 = strId2;
        mListener1 = l1;
        mListener2 = l2;
        mMessageId = messageId;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        View view = LayoutInflater.from(getActivity()).inflate(R.layout.custom_live_dialog, null);
        builder.setView(view);
        builder.setMessage(mMessageId);

        Button btn1 = (Button) view.findViewById(R.id.btn1);
        btn1.setText(getString(mBtnStrId1));
        btn1.setOnClickListener(mListener1);
        if (mBtnStrId1 < 0) {
            btn1.setVisibility(View.GONE);
        }

        Button btn2 = (Button) view.findViewById(R.id.btn2);
        btn2.setText(getString(mBtnStrId2));
        btn2.setOnClickListener(mListener2);
        if (mBtnStrId2 < 0) {
            btn2.setVisibility(View.GONE);
        }

        return builder.create();
    }

}
