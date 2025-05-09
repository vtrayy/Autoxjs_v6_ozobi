package com.stardust.autojs.core.ui.widget;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.util.AttributeSet;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.drawerlayout.widget.DrawerLayout;

import com.stardust.autojs.R;

public class JsToolbar extends androidx.appcompat.widget.Toolbar {

    public JsToolbar(Context context) {
        super(context);
    }

    public JsToolbar(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public JsToolbar(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setupWithDrawer(DrawerLayout drawerLayout) {
        ActionBarDrawerToggle drawerToggle = new ActionBarDrawerToggle(getActivity(), drawerLayout, this, R.string.text_drawer_open,
                R.string.text_drawer_close);
        drawerToggle.syncState();
        drawerLayout.addDrawerListener(drawerToggle);
    }

    private Activity getActivity() {
        Context context = getContext();
        while (!(context instanceof Activity)) {
            if (context instanceof ContextWrapper) {
                context = ((ContextWrapper) context).getBaseContext();
            } else {
                return null;
            }
        }
        return (Activity) context;
    }
}
