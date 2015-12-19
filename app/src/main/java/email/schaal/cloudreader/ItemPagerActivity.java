/*
 * Copyright (C) 2015 Daniel Schaal <daniel@schaal.email>
 *
 * This file is part of Cloudreader.
 *
 * Cloudreader is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Cloudreader is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Cloudreader.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package email.schaal.cloudreader;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.ColorInt;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v13.app.FragmentPagerAdapter;
import android.support.v13.app.FragmentStatePagerAdapter;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import java.util.WeakHashMap;

import email.schaal.cloudreader.database.Queries;
import email.schaal.cloudreader.model.Feed;
import email.schaal.cloudreader.model.Item;
import email.schaal.cloudreader.model.TemporaryFeed;
import email.schaal.cloudreader.util.FaviconUtils;

public class ItemPagerActivity extends RealmActivity {
    private static final String TAG = ItemPagerActivity.class.getSimpleName();

    public static final String POSITION = "position";

    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * {@link android.support.v4.app.FragmentStatePagerAdapter}.
     */
    private SectionsPagerAdapter mSectionsPagerAdapter;

    /**
     * The {@link ViewPager} that will host the section contents.
     */
    private ViewPager mViewPager;
    private TemporaryFeed temporaryFeed;
    private Toolbar toolbar;

    @ColorInt private int defaultToolbarColor;
    private Item item;

    private MenuItem menuItemMarkRead;
    private MenuItem menuItemMarkStarred;
    private FloatingActionButton fab;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_item_pager);

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        TypedArray typedArray = obtainStyledAttributes(new int[] { R.attr.colorPrimary });
        try {
            defaultToolbarColor = typedArray.getColor(0, 0);
        } finally {
            typedArray.recycle();
        }

        int position = getIntent().getIntExtra(POSITION, 0);
        temporaryFeed = getRealm().where(TemporaryFeed.class).findFirst();

        //noinspection ConstantConditions
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(temporaryFeed.getTitle());

        mSectionsPagerAdapter = new SectionsPagerAdapter(getFragmentManager());

        fab = (FloatingActionButton) findViewById(R.id.fab_open_in_browser);
        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        ViewPager.OnPageChangeListener pageChangeListener = new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                Log.d(TAG, "onPageSelected: " + position);
                item = getItemForPosition(position);
                setItemUnread(false);

                Feed feed = Item.feed(item);
                setActionBarColors(feed);
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        };

        pageChangeListener.onPageSelected(position);

        mViewPager.addOnPageChangeListener(pageChangeListener);
        mViewPager.setCurrentItem(position, false);
    }

    public Item getItemForPosition(int position) {
        return temporaryFeed.getItems().get(position);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_item_pager, menu);

        menuItemMarkRead = menu.findItem(R.id.action_mark_read);
        menuItemMarkStarred = menu.findItem(R.id.action_mark_starred);

        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menuItemMarkRead.setChecked(!item.isUnread());
        menuItemMarkRead.setIcon(menuItemMarkRead.isChecked() ? R.drawable.ic_check_box : R.drawable.ic_check_box_outline_blank);
        menuItemMarkRead.setIcon(menuItemMarkRead.isChecked() ? R.drawable.ic_check_box : R.drawable.ic_check_box_outline_blank);

        menuItemMarkStarred.setChecked(item.isStarred());
        menuItemMarkStarred.setIcon(menuItemMarkStarred.isChecked() ? R.drawable.ic_star : R.drawable.ic_star_outline);
        menuItemMarkStarred.setIcon(menuItemMarkStarred.isChecked() ? R.drawable.ic_star : R.drawable.ic_star_outline);

        return super.onPrepareOptionsMenu(menu);
    }

    private void setItemUnread(boolean unread) {
        Queries.getInstance().setItemUnreadState(getRealm(), this.item, unread, null);
        invalidateOptionsMenu();
    }

    private void setItemStarred(boolean starred) {
        Queries.getInstance().setItemStarredState(getRealm(), this.item, starred, null);
        invalidateOptionsMenu();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if(id == R.id.action_mark_read) {
            setItemUnread(!this.item.isUnread());
            return true;
        } else if(id == R.id.action_mark_starred) {
            setItemStarred(!this.item.isStarred());
        }

        return super.onOptionsItemSelected(item);
    }

    public void setActionBarColors(final Feed feed) {
        toolbar.setBackgroundColor(ContextCompat.getColor(ItemPagerActivity.this, R.color.primary));
        FaviconUtils.getInstance().loadFavicon(this, feed, new FaviconUtils.PaletteBitmapAsyncListener() {
            @Override
            public void onGenerated(@Nullable Palette palette, @Nullable Bitmap bitmap) {
                setColorsFromPalette(palette);
            }
        });
    }

    private void setColorsFromPalette(@Nullable Palette palette) {
        int toolbarColor;
        if (palette != null) {
            toolbarColor = palette.getDarkVibrantColor(defaultToolbarColor);
        } else {
            toolbarColor = defaultToolbarColor;
        }
        toolbar.setBackgroundColor(toolbarColor);
        fab.setBackgroundTintList(ColorStateList.valueOf(toolbarColor));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            int statusbarColor = Color.rgb(
                    (int) (Color.red(toolbarColor) * 0.7),
                    (int) (Color.green(toolbarColor) * 0.7),
                    (int) (Color.blue(toolbarColor) * 0.7)
            );
            getWindow().setStatusBarColor(statusbarColor);
        }
    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentStatePagerAdapter {
        private final WeakHashMap<Integer, ItemPageFragment> fragments;

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
            fragments = new WeakHashMap<>(getCount());
        }

        @Override
        public Fragment getItem(int position) {
            ItemPageFragment fragment;
            if(fragments.containsKey(position))
                fragment = fragments.get(position);
            else {
                fragment = ItemPageFragment.newInstance(position);
                fragments.put(position, fragment);
            }
            return fragment;
        }

        @Override
        public int getCount() {
            return temporaryFeed.getItems().size();
        }
    }
}
