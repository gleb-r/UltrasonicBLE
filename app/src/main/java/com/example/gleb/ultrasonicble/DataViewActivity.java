package com.example.gleb.ultrasonicble;

import android.os.Bundle;
import android.support.constraint.ConstraintLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.wdullaer.materialdatetimepicker.date.DatePickerDialog;

import java.text.SimpleDateFormat;
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
        mLayoutInflater = getLayoutInflater();
        mData = UltraHeightSingleton.get(this).getData();
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mRecyclerView.setAdapter(new CustomAdapter());
        int itemsCount = UltraHeightSingleton.get(this).getItemsCount();
        String subtitle = getString(R.string.subtitle_format, itemsCount);
        getSupportActionBar().setSubtitle(subtitle);
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
        Calendar[] selectable_days = new Calendar[4];
        for (int i = 0; i < 4; i++) {
            Calendar day = Calendar.getInstance();
            day.set(2017,9,26-i*2);
            selectable_days[i] = day;
        }
        datePickerDialog.setSelectableDays(selectable_days);
        datePickerDialog.show(getFragmentManager(),"DatePickerDialog");

    }

    @Override
    protected void onResume() {
        super.onResume();
        DatePickerDialog datePickerDialog =
                (DatePickerDialog)getFragmentManager().findFragmentByTag("DatePickerDialog");
        if (datePickerDialog!=null) {
            datePickerDialog.setOnDateSetListener(this);
        }
    }

    @Override
    public void onDateSet(DatePickerDialog view, int year, int monthOfYear, int dayOfMonth) {
        String date = "You picked the following date: " + dayOfMonth + "/" + monthOfYear + "/" + year;
        Log.i(TAG,date);

    }

    private class CustomAdapter extends RecyclerView.Adapter<CustomViewHolder> {


        @Override
        public CustomViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = mLayoutInflater.inflate(R.layout.item_layout, parent, false);
            return new CustomViewHolder(view);
        }

        @Override
        public void onBindViewHolder(CustomViewHolder holder, int position) {
            if (mData != null && mData.size() > 0) {
                UltraHeight ultraHeight = mData.get(position);
                if (ultraHeight != null) {
                    if (position % 2 == 0) {
                        holder.mLayout.setBackgroundResource(R.color.grey_row);
                    } else {
                        holder.mLayout.setBackgroundResource(R.color.white_row);
                    }
                    holder.tvNumber.setText(String.valueOf(position));
                    holder.tvHeight.setText(String.format(Locale.US, "%.0f", ultraHeight.getHeight()));
                    holder.tvSpeed.setText(String.format(Locale.US, "%.1f", ultraHeight.getSpeed()));
                    String dateAndTimeStr = new SimpleDateFormat("dd/MM/yy HH:mm:ss", Locale.US).format(ultraHeight.getDate());
                    holder.tvDateAndTime.setText(dateAndTimeStr);
                } else {
                    Log.i(TAG, "ultraHeight is null");
                }
            }
        }


        @Override
        public int getItemCount() {
            return mData.size();
        }
    }

    private class CustomViewHolder extends RecyclerView.ViewHolder {
        private ConstraintLayout mLayout;
        private TextView tvNumber;
        private TextView tvHeight;
        private TextView tvSpeed;
        private TextView tvDateAndTime;

        public CustomViewHolder(View itemView) {
            super(itemView);
            this.mLayout = (ConstraintLayout) itemView.findViewById(R.id.layout_row);
            this.tvNumber = (TextView) itemView.findViewById(R.id.tv_item_number);
            this.tvHeight = (TextView) itemView.findViewById(R.id.tv_item_height);
            this.tvSpeed = (TextView) itemView.findViewById(R.id.tv_item_speed);
            this.tvDateAndTime = (TextView) itemView.findViewById(R.id.tv_item_date_n_time);
        }


    }
}
