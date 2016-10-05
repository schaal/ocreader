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

import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Intent;
import android.content.res.TypedArray;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.ColorInt;
import android.support.v13.app.FragmentStatePagerAdapter;
import android.support.v4.graphics.ColorUtils;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import java.util.WeakHashMap;

import email.schaal.ocreader.database.Queries;
import email.schaal.ocreader.database.model.Item;
import email.schaal.ocreader.database.model.TemporaryFeed;
import email.schaal.ocreader.util.FaviconLoader;
import email.schaal.ocreader.util.FeedColors;
import email.schaal.ocreader.view.ProgressFloatingActionButton;
import io.realm.Sort;

public class ItemPagerActivity extends RealmActivity {
    private static final String TAG = ItemPagerActivity.class.getName();

    public static final String POSITION = "position";
    public static final int REQUEST_CODE = 2;
    public static final String EXTRA_CURRENT_POSITION = "email.schaal.ocreader.extra.CURRENT_POSIION";

    private Sort order;
    private String sortField;

    private TemporaryFeed temporaryFeed;
    private Toolbar toolbar;
    private ProgressFloatingActionButton fab;

    @ColorInt private int defaultToolbarColor;
    @ColorInt private int defaultAccent;
    private Item item;

    private MenuItem menuItemMarkRead;
    private MenuItem menuItemMarkStarred;
    private ViewPager mViewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_item_pager);

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        order = Preferences.ORDER.getOrder(PreferenceManager.getDefaultSharedPreferences(this));
        sortField = Preferences.SORT_FIELD.getString(PreferenceManager.getDefaultSharedPreferences(this));

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
        getSupportActionBar().setTitle(temporaryFeed.getName());

        final SectionsPagerAdapter mSectionsPagerAdapter = new SectionsPagerAdapter(getFragmentManager());

        fab = (ProgressFloatingActionButton) findViewById(R.id.fab_open_in_browser);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(item != null && item.getUrl() != null) {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(item.getUrl()));
                    startActivity(intent);
                }
            }
        });

        mViewPager = (ViewPager) findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        ViewPager.OnPageChangeListener pageChangeListener = new MyOnPageChangeListener(mSectionsPagerAdapter);

        mViewPager.addOnPageChangeListener(pageChangeListener);

        // The initial position is 0, so the pageChangeListener won't be called when setting the position to 0
        if(position == 0)
            pageChangeListener.onPageSelected(position);
        mViewPager.setCurrentItem(position, false);
    }

    private void shareArticle() {
        if(item.getUrl() != null) {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_TEXT, item.getUrl());
            startActivity(Intent.createChooser(shareIntent, getString(R.string.share_article)));
        }
    }

    public Item getItemForPosition(int position) {
        return temporaryFeed.getItems().sort(sortField, order).get(position);
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
        Queries.setItemsUnread(getRealm(), unread, this.item);
        invalidateOptionsMenu();
    }

    private void setItemStarred(boolean starred) {
        Queries.setItemsStarred(getRealm(), starred, this.item);
        invalidateOptionsMenu();
    }

    public void updateResult() {
        Intent result = new Intent();
        result.putExtra(EXTRA_CURRENT_POSITION, mViewPager.getCurrentItem());
        setResult(RESULT_OK, result);
    }

    @Override
    public void onBackPressed() {
        updateResult();
        super.onBackPressed();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                updateResult();
                return super.onOptionsItemSelected(item);
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

    private class SectionsPagerAdapter extends FragmentStatePagerAdapter {
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

    private class MyOnPageChangeListener implements ViewPager.OnPageChangeListener {
        public static final int DURATION = 250;
        private final SectionsPagerAdapter mSectionsPagerAdapter;

        private final ValueAnimator.AnimatorUpdateListener animatorUpdateListener = new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                int backgroundColor = (int) animation.getAnimatedValue();

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    int statusbarColor = changeLightness(backgroundColor, 0.7f);
                    getWindow().setStatusBarColor(statusbarColor);
                }

                toolbar.setBackgroundColor(backgroundColor);
            }
        };

        private int changeLightness(int backgroundColor, float lightnessChange) {
            float[] hsl = new float[3];
            ColorUtils.colorToHSL(backgroundColor, hsl);
            hsl[2] *= lightnessChange;
            return ColorUtils.HSLToColor(hsl);
        }

        private int fabColorFrom;
        private int fabColorTo;

        private int colorFrom;
        private int colorTo;

        private float progressFrom;
        private float progressTo;

        private final ArgbEvaluator argbEvaluator = new ArgbEvaluator();

        private boolean firstRun = true;
        private final FaviconLoader.FeedColorsListener toListener = new FaviconLoader.FeedColorsListener() {
            @Override
            public void onGenerated(FeedColors feedColors) {
                colorTo = FeedColors.get(feedColors, FeedColors.Type.TEXT, defaultToolbarColor);
                fabColorTo = FeedColors.get(feedColors, FeedColors.Type.BACKGROUND, defaultAccent);

                if(firstRun) {
                    firstRun = false;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        int statusbarColor = changeLightness(colorTo, 0.7f);
                        getWindow().setStatusBarColor(statusbarColor);
                    }

                    toolbar.setBackgroundColor(colorTo);
                    fab.setBackgroundColor(fabColorTo);
                } else {
                    ValueAnimator animator = ValueAnimator.ofInt(colorFrom, colorTo).setDuration(DURATION);
                    animator.setEvaluator(argbEvaluator);
                    animator.addUpdateListener(animatorUpdateListener);
                    animator.start();

                    ObjectAnimator fabAnimator = ObjectAnimator.ofInt(fab, "backgroundColor", fabColorFrom, fabColorTo).setDuration(DURATION);
                    fabAnimator.setEvaluator(argbEvaluator);
                    fabAnimator.start();
                }
            }

            @Override
            public void onStart() {
                colorFrom = colorTo;
                fabColorFrom = fabColorTo;
            }
        };

        public MyOnPageChangeListener(SectionsPagerAdapter mSectionsPagerAdapter) {
            this.mSectionsPagerAdapter = mSectionsPagerAdapter;
            colorTo = defaultToolbarColor;
            fabColorTo = defaultAccent;
        }

        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        }

        @Override
        public void onPageSelected(int position) {
            item = getItemForPosition(position);
            setItemUnread(false);

            new FaviconLoader.Builder(fab, item.getFeed())
                    .withGenerateFallbackImage(false)
                    .withPlaceholder(R.drawable.ic_open_in_browser)
                    .build()
                    .load(ItemPagerActivity.this, toListener);

            progressFrom = progressTo;
            progressTo = (float) (position + 1) / (float) mSectionsPagerAdapter.getCount();
            fab.show();

            ObjectAnimator progressAnimator = ObjectAnimator
                    .ofFloat(fab, "progress", progressFrom, progressTo)
                    .setDuration(DURATION);
            progressAnimator.start();
        }

        @Override
        public void onPageScrollStateChanged(int state) {
        }
    }
}
