/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.lenovoparts.widget;

import android.content.Context;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import org.lineageos.lenovoparts.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Single choice list where each row carries details: a radio button, the
 * name with a format line below, and a value on the right.
 */
public class CodecListPreference extends Preference {

    public static final class Row {
        final String key;
        final CharSequence name;
        final CharSequence format;
        final CharSequence rate;

        public Row(String key, CharSequence name, CharSequence format, CharSequence rate) {
            this.key = key;
            this.name = name;
            this.format = format;
            this.rate = rate;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Row)) {
                return false;
            }
            Row r = (Row) o;
            return key.equals(r.key) && TextUtils.equals(name, r.name)
                    && TextUtils.equals(format, r.format) && TextUtils.equals(rate, r.rate);
        }

        @Override
        public int hashCode() {
            return Objects.hash(key, name);
        }
    }

    public interface Listener {
        void onRowSelected(String key);
    }

    private List<Row> mRows = new ArrayList<>();
    private String mSelected;
    private Listener mListener;

    public CodecListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setLayoutResource(R.layout.preference_codec_list);
        setSelectable(false);
    }

    public void setListener(Listener listener) {
        mListener = listener;
    }

    /** Does nothing when the rows and the selection are unchanged. */
    public void setRows(List<Row> rows, String selected) {
        if (mRows.equals(rows) && TextUtils.equals(mSelected, selected)) {
            return;
        }
        mRows = new ArrayList<>(rows);
        mSelected = selected;
        notifyChanged();
    }

    @Override
    public void onBindViewHolder(@NonNull PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        holder.setDividerAllowedAbove(false);
        holder.setDividerAllowedBelow(false);
        ViewGroup list = (ViewGroup) holder.findViewById(R.id.codec_rows);
        list.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(getContext());
        for (Row row : mRows) {
            View v = inflater.inflate(R.layout.codec_row, list, false);
            ((RadioButton) v.findViewById(R.id.codec_radio))
                    .setChecked(row.key.equals(mSelected));
            ((TextView) v.findViewById(R.id.codec_name)).setText(row.name);
            TextView format = v.findViewById(R.id.codec_format);
            format.setText(row.format);
            format.setVisibility(TextUtils.isEmpty(row.format) ? View.GONE : View.VISIBLE);
            TextView rate = v.findViewById(R.id.codec_rate);
            rate.setText(row.rate);
            rate.setVisibility(TextUtils.isEmpty(row.rate) ? View.GONE : View.VISIBLE);
            v.setOnClickListener(x -> {
                if (mListener != null && !row.key.equals(mSelected)) {
                    mListener.onRowSelected(row.key);
                }
            });
            list.addView(v);
        }
    }
}
