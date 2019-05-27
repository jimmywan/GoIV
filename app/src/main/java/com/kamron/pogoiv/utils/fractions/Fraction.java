package com.kamron.pogoiv.utils.fractions;

import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;


public abstract class Fraction {

    FractionManager fractionManager;
    private boolean isCreated = false;

    public enum Anchor {
        TOP(Gravity.TOP),
        BOTTOM(Gravity.BOTTOM);

        private int gravity;

        Anchor(int gravity) {
            this.gravity = gravity;
        }

        public int getGravity() {
            return gravity;
        }
    }

    public final void create(@NonNull View rootView, @Nullable FractionManager fractionManager) {
        isCreated = false;
        this.fractionManager = fractionManager;
        onCreate(rootView);
        isCreated = true;
        onCreated();
    }

    public abstract @LayoutRes int getLayoutResId();

    public abstract void onCreate(@NonNull View rootView);

    public void onCreated() {

    }

    public abstract void onDestroy();

    /**
     * Vertical positioning of this Fraction container. If TOP to anchors to the top of the screen,
     * if BOTTOM anchors to the bottom of the screen.
     * @return The vertical side to anchor to
     */
    public abstract Anchor getAnchor();

    /**
     * Get the vertical distance of this Fraction container from its anchor.
     * See {@link #getAnchor()}.
     * @return number of pixels to offset from the anchor
     */
    public abstract int getVerticalOffset(@NonNull DisplayMetrics displayMetrics);

    public boolean isCreated() {
        return this.isCreated;
    }
}
