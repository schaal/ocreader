/*
 * Copyright (C) 2015-2016 Daniel Schaal <daniel@schaal.email>
 *
 * This file is part of OCReader.
 *
 * OCReader is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * OCReader is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with OCReader.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package email.schaal.ocreader;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Intent;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.ColorInt;
import android.support.annotation.Nullable;
import android.support.v13.app.FragmentStatePagerAdapter;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import java.util.WeakHashMap;

import email.schaal.ocreader.database.Queries;
import email.schaal.ocreader.model.Feed;
import email.schaal.ocreader.model.Item;
import email.schaal.ocreader.model.TemporaryFeed;
import email.schaal.ocreader.util.FaviconUtils;
import email.schaal.ocreader.view.ProgressFloatingActionButton;

public class ItemPagerActivity extends RealmActivity {
    private static final String TAG = ItemPagerActivity.class.getName();

    public static final String POSITION = "position";

    private TemporaryFeed temporaryFeed;
    private Toolbar toolbar;
    private ProgressFloatingActionButton fab;

    @ColorInt private int defaultToolbarColor;
    @ColorInt private int defaultAccent;
    private Item item;

    private MenuItem menuItemMarkRead;
    private MenuItem menuItemMarkStarred;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_item_pager);

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        TypedArray typedArray = obtainStyledAttributes(new int[] { R.attr.colorPrimary, R.attr.colorAccent });
        try {
            defaultToolbarColor = typedArray.getColor(0, 0);
            //noinspection ResourceType
            defaultAccent = typedArray.getColor(1 , 0);
        } finally {
            typedArray.recycle();
        }

        int position = getIntent().getIntExtra(POSITION, 0);
        temporaryFeed = getRealm().where(TemporaryFeed.class).findFirst();

        //noinspection ConstantConditions
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(temporaryFeed.getTitle());

        final SectionsPagerAdapter mSectionsPagerAdapter = new SectionsPagerAdapter(getFragmentManager());

        fab = (ProgressFloatingActionButton) findViewById(R.id.fab_open_in_browser);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(item != null) {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(item.getUrl()));
                    startActivity(intent);
                }
            }
        });

        ViewPager mViewPager = (ViewPager) findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        ViewPager.OnPageChangeListener pageChangeListener = new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                item = getItemForPosition(position);
                setItemUnread(false);

                setActionBarColors(item.feed());

                fab.setProgress((float)(position+1) / (float)mSectionsPagerAdapter.getCount());
                fab.show();
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        };

        pageChangeListener.onPageSelected(position);

        mViewPager.addOnPageChangeListener(pageChangeListener);
        mViewPager.setCurrentItem(position, false);
    }

    private void shareArticle() {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, item.getUrl());
        startActivity(Intent.createChooser(shareIntent, getString(R.string.share_article)));
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

        menuItemMarkStarred.setChecked(item.isStarred());
        menuItemMarkStarred.setIcon(menuItemMarkStarred.isChecked() ? R.drawable.ic_star : R.drawable.ic_star_outline);

        return super.onPrepareOptionsMenu(menu);
    }

    private void setItemUnread(boolean unread) {
        Queries.getInstance().setItemsUnread(getRealm(), unread, this.item);
        invalidateOptionsMenu();
    }

    private void setItemStarred(boolean starred) {
        Queries.getInstance().setItemStarred(getRealm(), starred, this.item);
        invalidateOptionsMenu();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_mark_read:
                setItemUnread(!this.item.isUnread());
                return true;
            case R.id.action_mark_starred:
                setItemStarred(!this.item.isStarred());
                return true;
            case R.id.action_share_article:
                shareArticle();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void setActionBarColors(final Feed feed) {
        toolbar.setBackgroundColor(ContextCompat.getColor(ItemPagerActivity.this, R.color.primary));
        FaviconUtils.getInstance().loadFavicon(this, feed, new FaviconUtils.PaletteBitmapAsyncListener() {
            @Override
            public void onGenerated(@Nullable Palette palette, @Nullable Bitmap bitmap) {
                setColorsFromPalette(palette);
                if(bitmap != null)
                    fab.setImageBitmap(bitmap);
                else
                    fab.setImageResource(R.drawable.ic_open_in_browser);
            }
        });
    }

    private void setColorsFromPalette(@Nullable Palette palette) {
        int toolbarColor;
        int fabColor;
        if (palette != null) {
            toolbarColor = FaviconUtils.getTextColor(palette, defaultToolbarColor);
            fabColor = palette.getLightMutedColor(defaultAccent);
        } else {
            toolbarColor = defaultToolbarColor;
            fabColor = defaultAccent;
        }
        toolbar.setBackgroundColor(toolbarColor);
        //fab.setBackgroundTintList(ColorStateList.valueOf(fabColor));
        fab.setBackgroundColor(fabColor);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            int statusbarColor = Color.rgb(
                    (int) (Color.red(toolbarColor) * 0.7),
                    (int) (Color.green(toolbarColor) * 0.7),
                    (int) (Color.blue(toolbarColor) * 0.7)
            );
            getWindow().setStatusBarColor(statusbarColor);
        }
    }

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
