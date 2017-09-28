package com.example.gleb.ultrasonicble;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.wdullaer.materialdatetimepicker.date.DatePickerDialog;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class DataViewActivity extends AppCompatActivity implements DatePickerDialog.OnDateSetListener {

    private RecyclerView mRecyclerView;
    private LayoutInflater mLayoutInflater;
    private List<UltraHeight> mData;
    private final static String TAG = DataViewActivity.class.getName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_data_view);
        mRecyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        UltraHeight testUltraHeiht = new UltraHeight(100,15);

        mLayoutInflater = getLayoutInflater();
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mRecyclerView.setAdapter(new CustomAdapter(new ArrayList<UltraHeight>()));
        startDatePicker();
    }


    private void startDatePicker() {
        Calendar now = Calendar.getInstance();
        DatePickerDialog datePickerDialog = DatePickerDialog.newInstance(
                this,
                now.get(Calendar.YEAR),
                now.get(Calendar.MONTH),
                now.get(Calendar.DATE)
        );
        Calendar[] selectable_days = UltraHeightSingleton.get(this).getActiveDays();
        if (selectable_days != null) {
            datePickerDialog.setSelectableDays(selectable_days);
        }
        datePickerDialog.show(getFragmentManager(), "DatePickerDialog");

    }


    @Override
    protected void onResume() {
        super.onResume();
        DatePickerDialog datePickerDialog =
                (DatePickerDialog) getFragmentManager().findFragmentByTag("DatePickerDialog");
        if (datePickerDialog != null) {
            datePickerDialog.setOnDateSetListener(this);
        }
    }

    @Override
    public void onDateSet(DatePickerDialog view, int year, int monthOfYear, int dayOfMonth) {
        // Нумерация месяцев начинаеться с 0
        mData = UltraHeightSingleton.get(this).getDataOnDate(year, ++monthOfYear, dayOfMonth);
        mRecyclerView.setAdapter(new CustomAdapter(mData));

        String subtitle = getString(
                R.string.subtitle_format,
                year,
                monthOfYear,
                dayOfMonth,
                mData.size());
        try {
            getSupportActionBar().setSubtitle(subtitle);
        } catch (NullPointerException e) {
            Log.e(TAG, "Can't set subtitle " + e);
        }

    }

    private class CustomAdapter extends RecyclerView.Adapter<CustomViewHolder> {

        private List<UltraHeight> data;

        public CustomAdapter (List<UltraHeight> data) {
            this.data = data;
        }


        @Override
        public CustomViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = mLayoutInflater.inflate(R.layout.item_layout2, parent, false);
            return new CustomViewHolder(view);
        }

        @Override
        public void onBindViewHolder(CustomViewHolder holder, int position) {
            if (data != null && data.size() > 0) {
                UltraHeight ultraHeight = data.get(position);
                if (ultraHeight != null) {
                    if (position % 2 == 0) {
                        holder.mLayout.setBackgroundResource(R.color.grey_row);
                    } else {
                        holder.mLayout.setBackgroundResource(R.color.white_row);
                    }
                    holder.tvNumber.setText(String.valueOf(position));
                    holder.tvHeight.setText(String.format(Locale.US, "%.0f", ultraHeight.getHeight()));
                    holder.tvSpeed.setText(String.format(Locale.US, "%.1f", ultraHeight.getSpeed()));
                    String timeStr = new SimpleDateFormat("HH:mm:ss", Locale.US).format(ultraHeight.getDate());
                    holder.tvTime.setText(timeStr);
                } else {
                    Log.i(TAG, "ultraHeight is null");
                }
            }
        }


        @Override
        public int getItemCount() {
            return data.size();
        }
    }

    private class CustomViewHolder extends RecyclerView.ViewHolder {
        private RelativeLayout mLayout;
        private TextView tvNumber;
        private TextView tvHeight;
        private TextView tvSpeed;
        private TextView tvTime;

        public CustomViewHolder(View itemView) {
            super(itemView);
            this.mLayout = (RelativeLayout) itemView.findViewById(R.id.layout_row);
            this.tvNumber = (TextView) itemView.findViewById(R.id.tv_item_number);
            this.tvHeight = (TextView) itemView.findViewById(R.id.tv_item_height);
            this.tvSpeed = (TextView) itemView.findViewById(R.id.tv_item_speed);
            this.tvTime = (TextView) itemView.findViewById(R.id.tv_item_date_n_time);
        }


    }
}
