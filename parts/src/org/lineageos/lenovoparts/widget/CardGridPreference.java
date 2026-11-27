/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.lenovoparts.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

    private CharSequence[] mLabels = new CharSequence[0];
    private CharSequence[][] mText = new CharSequence[0][2];
    private View[] mCards;
    private boolean mGraphVisible;
    private PowerGraphView mGraph;
    private boolean mLoadGraphVisible;
    private ViewGroup mLoadCard;

    public CardGridPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setLayoutResource(R.layout.card_grid);
        setSelectable(false);
    }

    /** Card titles, in order; call before the preference is shown. */
    public void setCards(int... labels) {
        CharSequence[] text = new CharSequence[labels.length];
        for (int i = 0; i < labels.length; i++) {
            text[i] = getContext().getText(labels[i]);
        }
        setCardLabels(text);
    }

    /** Card titles as text; does nothing when they are unchanged. */
    public void setCardLabels(CharSequence... labels) {
        if (java.util.Arrays.equals(mLabels, labels)) {
            return;
        }
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
        holder.findViewById(R.id.load_card)
                .setVisibility(mLoadGraphVisible ? View.VISIBLE : View.GONE);
        mLoadCard = (ViewGroup) holder.findViewById(R.id.load_card);

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
        if (grid.getTag() == null) {
            grid.setTag(Boolean.TRUE);
            grid.addOnLayoutChangeListener((v, l, t, r, b, ol, ot, or, ob) -> {
                float dp = v.getResources().getDisplayMetrics().density;
                int columns = (r - l) / dp >= 560 ? 3 : 2;
                if (grid.getColumnCount() != columns) {
                    v.post(() -> setColumns(grid, columns));
                }
            });
        }
        for (int i = 0; i < mLabels.length; i++) {
            apply(i);
        }
    }

    /*
     * GridLayout rejects a smaller column count while its children still hold
     * positions from the wider layout, so place the cards again.
     */
    private static void setColumns(GridLayout grid, int columns) {
        if (grid.getColumnCount() == columns) {
            return;
        }
        int count = grid.getChildCount();
        View[] children = new View[count];
        for (int i = 0; i < count; i++) {
            children[i] = grid.getChildAt(i);
        }
        grid.removeAllViews();
        grid.setColumnCount(columns);
        for (View child : children) {
            GridLayout.LayoutParams lp = (GridLayout.LayoutParams) child.getLayoutParams();
            lp.rowSpec = GridLayout.spec(GridLayout.UNDEFINED);
            lp.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            grid.addView(child, lp);
        }
    }

    public void setCard(int card, CharSequence value, CharSequence detail) {
        mText[card][0] = value;
        mText[card][1] = keepTogether(detail);
        apply(card);
    }

    public void setLoadGraphVisible(boolean visible) {
        mLoadGraphVisible = visible;
        notifyChanged();
    }

    public void refreshGraph() {
        if (mLoadCard != null) {
            for (int i = 0; i < mLoadCard.getChildCount(); i++) {
                mLoadCard.getChildAt(i).invalidate();
            }
        }
        if (mGraph != null) {
            mGraph.invalidate();
        }
    }

    /*
     * Lines only break between the " · " separated parts, so that a number
     * stays with its unit ("34.7 °C", "4.0 GB").
     */
    private static CharSequence keepTogether(CharSequence text) {
        if (text == null) {
            return null;
        }
        String[] parts = text.toString().split(" · ");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (sb.length() > 0) {
                sb.append(" · ");
            }
            sb.append(part.replace(' ', '\u00a0'));
        }
        return sb.toString();
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
