/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.lenovoparts.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.GridLayout;
import android.widget.TextView;

import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import org.lineageos.lenovoparts.R;

/**
 * Live readings as a grid of cards: two columns, three on a wide pane.
 * Optionally a power graph card below the grid.
 */
public class CardGridPreference extends Preference {

    private int[] mLabels = new int[0];
    private CharSequence[][] mText = new CharSequence[0][2];
    private View[] mCards;
    private boolean mGraphVisible;
    private PowerGraphView mGraph;

    public CardGridPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setLayoutResource(R.layout.card_grid);
        setSelectable(false);
    }

    /** Card titles, in order; call before the preference is shown. */
    public void setCards(int... labels) {
        mLabels = labels;
        mText = new CharSequence[labels.length][2];
        mCards = null;
        notifyChanged();
    }

    public void setGraphVisible(boolean visible) {
        mGraphVisible = visible;
        notifyChanged();
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        holder.setDividerAllowedAbove(false);
        holder.setDividerAllowedBelow(false);
        GridLayout grid = (GridLayout) holder.findViewById(R.id.cards);
        holder.findViewById(R.id.graph_card)
                .setVisibility(mGraphVisible ? View.VISIBLE : View.GONE);
        mGraph = (PowerGraphView) holder.findViewById(R.id.power_graph);

        if (mCards == null || grid.getChildCount() != mLabels.length) {
            grid.removeAllViews();
            mCards = new View[mLabels.length];
            LayoutInflater inflater = LayoutInflater.from(getContext());
            int margin = (int) (6 * getContext().getResources().getDisplayMetrics().density);
            for (int i = 0; i < mLabels.length; i++) {
                View card = inflater.inflate(R.layout.card_grid_item, grid, false);
                GridLayout.LayoutParams lp = new GridLayout.LayoutParams(
                        GridLayout.spec(GridLayout.UNDEFINED),
                        GridLayout.spec(GridLayout.UNDEFINED, 1f));
                lp.width = 0;
                lp.setMargins(margin, margin, margin, margin);
                card.setLayoutParams(lp);
                ((TextView) card.findViewById(R.id.card_label)).setText(mLabels[i]);
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
        for (int i = 0; i < mLabels.length; i++) {
            apply(i);
        }
    }

    public void setCard(int card, CharSequence value, CharSequence detail) {
        mText[card][0] = value;
        mText[card][1] = detail;
        apply(card);
    }

    public void refreshGraph() {
        if (mGraph != null) {
            mGraph.invalidate();
        }
    }

    private void apply(int card) {
        if (mCards == null || card >= mCards.length || mCards[card] == null) {
            return;
        }
        ((TextView) mCards[card].findViewById(R.id.card_value)).setText(mText[card][0]);
        TextView detail = mCards[card].findViewById(R.id.card_detail);
        detail.setText(mText[card][1]);
        detail.setVisibility(mText[card][1] == null ? View.GONE : View.VISIBLE);
    }
}
