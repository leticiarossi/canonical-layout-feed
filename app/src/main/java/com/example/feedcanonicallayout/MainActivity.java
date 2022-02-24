package com.example.feedcanonicallayout;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.util.Consumer;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.window.java.layout.WindowInfoTrackerCallbackAdapter;
import androidx.window.layout.DisplayFeature;
import androidx.window.layout.FoldingFeature;
import androidx.window.layout.FoldingFeature.Orientation;
import androidx.window.layout.WindowInfoTracker;
import androidx.window.layout.WindowLayoutInfo;

import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.navigationrail.NavigationRailView;

import java.util.List;
import java.util.concurrent.Executor;

public class MainActivity extends AppCompatActivity {

    private View container;
    private AdaptiveFeedFragment feedFragment;

    @Nullable
    private WindowInfoTrackerCallbackAdapter windowInfoTracker;
    private final Consumer<WindowLayoutInfo> stateContainer = new StateContainer();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Executor executor = command -> handler.post(() -> handler.post(command));
    private Configuration configuration;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        container = findViewById(R.id.feed_activity_container);
        DrawerLayout drawerLayout = findViewById(R.id.drawer_layout);
        NavigationView modalNavDrawer = findViewById(R.id.modal_nav_drawer);
        windowInfoTracker =
                new WindowInfoTrackerCallbackAdapter(WindowInfoTracker.getOrCreate(this));
        configuration = getResources().getConfiguration();
        BottomNavigationView bottomNav = findViewById(R.id.bottom_nav);
        NavigationRailView navRail = findViewById(R.id.nav_rail);
        NavigationView navDrawer = findViewById(R.id.nav_drawer);
        ExtendedFloatingActionButton navFab = findViewById(R.id.nav_fab);

        feedFragment = new AdaptiveFeedFragment();

        // Update navigation views according to screen width size.
        int screenWidth = configuration.screenWidthDp;
        AdaptiveUtils.updateNavigationViewLayout(
                screenWidth,
                drawerLayout,
                modalNavDrawer,
                /* fab= */ null,
                bottomNav,
                navRail,
                navDrawer,
                navFab);

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, feedFragment)
                .commit();
    }



    @Override
    public void onStart() {
        super.onStart();
        if (windowInfoTracker != null) {
            windowInfoTracker.addWindowLayoutInfoListener(this, executor, stateContainer);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (windowInfoTracker != null) {
            windowInfoTracker.removeWindowLayoutInfoListener(stateContainer);
        }
    }

    private class StateContainer implements Consumer<WindowLayoutInfo> {

        public StateContainer() {}

        @Override
        public void accept(WindowLayoutInfo windowLayoutInfo) {
            if (feedFragment == null) {
                return;
            }

            if (configuration.screenWidthDp < AdaptiveUtils.MEDIUM_SCREEN_WIDTH_SIZE) {
                feedFragment.setClosedLayout();
            } else {
                List<DisplayFeature> displayFeatures = windowLayoutInfo.getDisplayFeatures();
                boolean isClosed = true;

                for (DisplayFeature displayFeature : displayFeatures) {
                    if (displayFeature instanceof FoldingFeature) {
                        FoldingFeature foldingFeature = (FoldingFeature) displayFeature;
                        if (foldingFeature.getState().equals(FoldingFeature.State.HALF_OPENED)
                                || foldingFeature.getState().equals(FoldingFeature.State.FLAT)) {
                            Orientation orientation = foldingFeature.getOrientation();
                            if (orientation.equals(FoldingFeature.Orientation.VERTICAL)) {
                                int foldPosition = foldingFeature.getBounds().left;
                                int foldWidth = foldingFeature.getBounds().right - foldPosition;
                                // Device is open and fold is vertical.
                                feedFragment.setOpenLayout(foldPosition, foldWidth);
                            } else {
                                // Device is open and fold is horizontal.
                                feedFragment.setOpenLayout(container.getWidth() / 2, 0);
                            }
                            isClosed = false;
                        }
                    }
                }
                if (isClosed) {
                    if (configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
                        // Device is closed or not foldable and in portrait.
                        feedFragment.setClosedLayout();
                    } else {
                        // Device is closed or not foldable and in landscape.
                        feedFragment.setOpenLayout(container.getWidth() / 2, 0);
                    }
                }
            }
        }
    }
}