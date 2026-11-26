/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.lenovoparts.charging;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.GridLayout;
import android.widget.TextView;

import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import org.lineageos.lenovoparts.R;

/** Live power readings as a grid of cards, with the power graph below. */
public class PowerDashboardPreference extends Preference {

    static final int CARD_STATUS = 0;
    static final int CARD_POWER = 1;
    static final int CARD_CHARGER = 2;
    static final int CARD_PUMPS = 3;
    static final int CARD_TEMP = 4;
    static final int CARD_CAPACITY = 5;
    private static final int CARDS = 6;

    private static final int[] LABELS = {
        R.string.power_card_status, R.string.power_card_power, R.string.power_card_charger,
        R.string.power_card_pumps, R.string.power_card_temp, R.string.power_card_capacity,
    };

    private final CharSequence[][] mText = new CharSequence[CARDS][2];
    private View[] mCards;
    private PowerGraphView mGraph;

    public PowerDashboardPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setLayoutResource(R.layout.power_dashboard);
        setSelectable(false);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        holder.setDividerAllowedAbove(false);
        holder.setDividerAllowedBelow(false);
        GridLayout grid = (GridLayout) holder.findViewById(R.id.cards);
        mGraph = (PowerGraphView) holder.findViewById(R.id.power_graph);

        if (mCards == null || grid.getChildCount() != CARDS) {
            grid.removeAllViews();
            mCards = new View[CARDS];
            LayoutInflater inflater = LayoutInflater.from(getContext());
            for (int i = 0; i < CARDS; i++) {
                View card = inflater.inflate(R.layout.power_card, grid, false);
                GridLayout.LayoutParams lp = new GridLayout.LayoutParams(
                        GridLayout.spec(GridLayout.UNDEFINED),
                        GridLayout.spec(GridLayout.UNDEFINED, 1f));
                lp.width = 0;
                int margin = (int) (6 * getContext().getResources().getDisplayMetrics().density);
                lp.setMargins(margin, margin, margin, margin);
                card.setLayoutParams(lp);
                ((TextView) card.findViewById(R.id.card_label)).setText(LABELS[i]);
                grid.addView(card);
                mCards[i] = card;
            }
        }
        grid.addOnLayoutChangeListener((v, l, t, r, b, ol, ot, or, ob) -> {
            float dp = v.getResources().getDisplayMetrics().density;
            int columns = (r - l) / dp >= 560 ? 3 : 2;
            if (grid.getColumnCount() != columns) {
                v.post(() -> grid.setColumnCount(columns));
            }
        });
        for (int i = 0; i < CARDS; i++) {
            apply(i);
        }
    }

    void setCard(int card, CharSequence value, CharSequence detail) {
        mText[card][0] = value;
        mText[card][1] = detail;
        apply(card);
    }

    void refreshGraph() {
        if (mGraph != null) {
            mGraph.invalidate();
        }
    }

    private void apply(int card) {
        if (mCards == null || mCards[card] == null) {
            return;
        }
        ((TextView) mCards[card].findViewById(R.id.card_value)).setText(mText[card][0]);
        TextView detail = mCards[card].findViewById(R.id.card_detail);
        detail.setText(mText[card][1]);
        detail.setVisibility(mText[card][1] == null ? View.GONE : View.VISIBLE);
    }
}
