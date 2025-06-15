package com.cypher.zealth;

import android.graphics.Rect;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class TileSpacingDecoration extends RecyclerView.ItemDecoration {

    private final int spacingPx;

    public TileSpacingDecoration(int spacingDp) {

        this.spacingPx = -1;
        this.spacingDp = spacingDp;
    }

    private final int spacingDp;

    @Override
    public void getItemOffsets(@NonNull Rect outRect, @NonNull View view,
                               @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
        int px = (int) (spacingDp * parent.getResources().getDisplayMetrics().density);


        outRect.left = px / 2;
        outRect.right = px / 2;
        outRect.top = px / 2;
        outRect.bottom = px / 2;
    }
}
