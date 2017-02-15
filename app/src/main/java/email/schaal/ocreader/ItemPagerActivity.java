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
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.databinding.DataBindingUtil;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.v13.app.FragmentStatePagerAdapter;
import android.support.v4.graphics.ColorUtils;
import android.support.v4.view.ViewPager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import java.util.WeakHashMap;

import email.schaal.ocreader.database.Queries;
import email.schaal.ocreader.database.model.Item;
import email.schaal.ocreader.database.model.TemporaryFeed;
import email.schaal.ocreader.databinding.ActivityItemPagerBinding;
import email.schaal.ocreader.util.FaviconLoader;
import email.schaal.ocreader.util.FeedColors;
import io.realm.RealmResults;
import io.realm.Sort;

public class ItemPagerActivity extends RealmActivity {

    public static final int REQUEST_CODE = 2;
    public static final String EXTRA_CURRENT_POSITION = "email.schaal.ocreader.extra.CURRENT_POSIION";

    private ActivityItemPagerBinding binding;

    @ColorInt private int defaultToolbarColor;
    @ColorInt private int defaultAccent;
    private Item item;

    private MenuItem menuItemMarkRead;
    private MenuItem menuItemMarkStarred;
    private RealmResults<Item> items;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_item_pager);

        setSupportActionBar(binding.toolbarLayout.toolbar);

        final Sort order = Preferences.ORDER.getOrder(PreferenceManager.getDefaultSharedPreferences(this));
        final String sortField = Preferences.SORT_FIELD.getString(PreferenceManager.getDefaultSharedPreferences(this));

        final TemporaryFeed temporaryFeed = getRealm().where(TemporaryFeed.class).findFirst();

        items = temporaryFeed.getItems().sort(sortField, order);

        TypedArray typedArray = obtainStyledAttributes(new int[] { R.attr.colorPrimary, R.attr.colorAccent });
        try {
            defaultToolbarColor = typedArray.getColor(0, 0);
            //noinspection ResourceType
            defaultAccent = typedArray.getColor(1 , 0);
        } finally {
            typedArray.recycle();
        }

        int position = getIntent().getIntExtra(EXTRA_CURRENT_POSITION, 0);

        //noinspection ConstantConditions
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(temporaryFeed.getName());

        final SectionsPagerAdapter mSectionsPagerAdapter = new SectionsPagerAdapter(getFragmentManager());

        binding.fabOpenInBrowser.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(item != null && item.getUrl() != null) {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(item.getUrl()));
                    startActivity(intent);
                }
            }
        });

        binding.container.setAdapter(mSectionsPagerAdapter);

        ViewPager.OnPageChangeListener pageChangeListener = new MyOnPageChangeListener(mSectionsPagerAdapter);

        binding.container.addOnPageChangeListener(pageChangeListener);

        // The initial position is 0, so the pageChangeListener won't be called when setting the position to 0
        if(position == 0)
            pageChangeListener.onPageSelected(position);
        binding.container.setCurrentItem(position, false);
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
        return items.get(position);
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
        result.putExtra(EXTRA_CURRENT_POSITION, binding.container.getCurrentItem());
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

        SectionsPagerAdapter(FragmentManager fm) {
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
            return items.size();
        }
    }

    private class MyOnPageChangeListener implements ViewPager.OnPageChangeListener {
        static final int DURATION = 250;
        private final SectionsPagerAdapter mSectionsPagerAdapter;

        private final int currentNightMode;

        private final ValueAnimator.AnimatorUpdateListener animatorUpdateListener = new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                int backgroundColor = (int) animation.getAnimatedValue();

                setStatusbarColor(backgroundColor);
            }
        };

        private void setStatusbarColor(final int backgroundColor) {
            // Don't set the toolbar color when in night mode (white text and icon tint doesn't work with feed color in night mode)
            if (currentNightMode == Configuration.UI_MODE_NIGHT_NO) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    int statusbarColor = changeLightness(backgroundColor, 0.7f);
                    getWindow().setStatusBarColor(statusbarColor);
                }
                binding.toolbarLayout.toolbar.setBackgroundColor(backgroundColor);
            }
        }

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
            public void onGenerated(@NonNull FeedColors feedColors) {
                colorTo = feedColors.getColor(FeedColors.Type.TEXT, defaultToolbarColor);
                fabColorTo = feedColors.getColor(FeedColors.Type.BACKGROUND, defaultAccent);

                if(firstRun) {
                    firstRun = false;

                    setStatusbarColor(colorTo);

                    binding.fabOpenInBrowser.setBackgroundColor(fabColorTo);
                } else {
                    ValueAnimator animator = ValueAnimator.ofInt(colorFrom, colorTo).setDuration(DURATION);
                    animator.setEvaluator(argbEvaluator);
                    animator.addUpdateListener(animatorUpdateListener);
                    animator.start();

                    ObjectAnimator fabAnimator = ObjectAnimator.ofInt(binding.fabOpenInBrowser, "backgroundColor", fabColorFrom, fabColorTo).setDuration(DURATION);
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

        MyOnPageChangeListener(SectionsPagerAdapter mSectionsPagerAdapter) {
            this.mSectionsPagerAdapter = mSectionsPagerAdapter;
            colorTo = defaultToolbarColor;
            fabColorTo = defaultAccent;
            currentNightMode = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        }

        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        }

        @Override
        public void onPageSelected(int position) {
            item = getItemForPosition(position);
            setItemUnread(false);

            new FaviconLoader.Builder(binding.fabOpenInBrowser, item.getFeed())
                    .withGenerateFallbackImage(false)
                    .withPlaceholder(R.drawable.ic_open_in_browser)
                    .build()
                    .load(ItemPagerActivity.this, toListener);

            progressFrom = progressTo;
            progressTo = (float) (position + 1) / (float) mSectionsPagerAdapter.getCount();
            binding.fabOpenInBrowser.show();

            ObjectAnimator progressAnimator = ObjectAnimator
                    .ofFloat(binding.fabOpenInBrowser, "progress", progressFrom, progressTo)
                    .setDuration(DURATION);
            progressAnimator.start();
        }

        @Override
        public void onPageScrollStateChanged(int state) {
        }
    }
}
