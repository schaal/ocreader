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

import android.animation.AnimatorSet;
import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.annotation.TargetApi;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.databinding.DataBindingUtil;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.ColorInt;
import android.support.annotation.Keep;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.graphics.ColorUtils;
import android.support.v4.view.ViewPager;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.WebView;

import java.util.List;
import java.util.WeakHashMap;

import email.schaal.ocreader.database.Queries;
import email.schaal.ocreader.database.model.Item;
import email.schaal.ocreader.database.model.TemporaryFeed;
import email.schaal.ocreader.databinding.ActivityItemPagerBinding;
import email.schaal.ocreader.util.FaviconLoader;
import email.schaal.ocreader.util.FeedColors;
import io.realm.Sort;

public class ItemPagerActivity extends RealmActivity {

    public static final int REQUEST_CODE = 2;
    public static final String EXTRA_CURRENT_POSITION = "email.schaal.ocreader.extra.CURRENT_POSIION";

    private ActivityItemPagerBinding binding;

    @ColorInt private int defaultToolbarColor;
    @ColorInt private int defaultAccent;
    private Item item;

    private List<Item> items;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if(Build.VERSION.SDK_INT >= 24) {
            new WebView(this);
        }

        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_item_pager);

        setSupportActionBar(binding.toolbarLayout.toolbar);

        final Sort order = Preferences.ORDER.getOrder(PreferenceManager.getDefaultSharedPreferences(this));
        final String sortField = Preferences.SORT_FIELD.getString(PreferenceManager.getDefaultSharedPreferences(this));

        final String title;

        if(getIntent().hasExtra("ARG_ITEMS")) {
            title = "Test";
            items = getIntent().getParcelableArrayListExtra("ARG_ITEMS");
        } else {
            TemporaryFeed.updatePagerTemporaryFeed(getRealm());
            final TemporaryFeed temporaryFeed = TemporaryFeed.getPagerTemporaryFeed(getRealm());
            items = temporaryFeed.getItems().sort(sortField, order);
            title = temporaryFeed.getName();
        }

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
        binding.toolbarLayout.textViewTitle.setText(title);
        binding.toolbarLayout.textViewSubtitle.setText("");

        final SectionsPagerAdapter mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        binding.fabOpenInBrowser.setOnClickListener(v -> {
            if(item != null && item.getUrl() != null) {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(item.getUrl()));
                startActivity(intent);
            }
        });

        binding.fabMarkStarred.setOnClickListener(v -> setItemStarred(!item.isStarred()));

        binding.fabMarkAsRead.setOnClickListener(v -> setItemUnread(!item.isUnread()));

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
        getMenuInflater().inflate(R.menu.menu_item_pager, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.findItem(R.id.action_play_enclosure_media).setVisible(item.getEnclosureLink() != null);
        return super.onPrepareOptionsMenu(menu);
    }

    private void prepareFabBar() {
        binding.fabMarkAsRead.setImageResource(!item.isUnread() ? R.drawable.ic_check_box : R.drawable.ic_check_box_outline_blank);
        binding.fabMarkStarred.setImageResource(item.isStarred() ? R.drawable.ic_star : R.drawable.ic_star_outline);
    }

    private void setItemUnread(boolean unread) {
        Queries.setItemsUnread(getRealm(), unread, this.item);
        prepareFabBar();
    }

    private void setItemStarred(boolean starred) {
        Queries.setItemsStarred(getRealm(), starred, this.item);
        prepareFabBar();
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
            case R.id.action_play_enclosure_media:
                this.item.play(this);
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
                fragment = ItemPageFragment.newInstance(getItemForPosition(position));
                fragments.put(position, fragment);
            }
            return fragment;
        }

        @Override
        public int getCount() {
            return items.size();
        }
    }

    private class StatusBarChanger {
        @Keep
        void setStatusBarColor(final int backgroundColor) {
            binding.toolbarLayout.toolbar.setBackgroundColor(backgroundColor);
        }
    }

    private class StatusBarChangerLollipop extends StatusBarChanger {
        private final float[] hsl = new float[3];

        private int changeLightness(int backgroundColor, float lightnessChange) {
            ColorUtils.colorToHSL(backgroundColor, hsl);
            hsl[2] *= lightnessChange;
            return ColorUtils.HSLToColor(hsl);
        }

        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
        @Keep
        @Override
        void setStatusBarColor(int backgroundColor) {
            int statusbarColor = changeLightness(backgroundColor, 0.7f);
            getWindow().setStatusBarColor(statusbarColor);
            super.setStatusBarColor(backgroundColor);
        }
    }

    private class MyOnPageChangeListener implements ViewPager.OnPageChangeListener {
        static final int DURATION = 250;
        private final SectionsPagerAdapter mSectionsPagerAdapter;

        private final int currentNightMode;
        private final StatusBarChanger statusBarChanger;

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

                    if(currentNightMode == Configuration.UI_MODE_NIGHT_NO)
                        statusBarChanger.setStatusBarColor(colorTo);

                    binding.fabLayout.setFabBackgroundColor(fabColorTo);
                } else {
                    ObjectAnimator fabAnimator =
                            ObjectAnimator
                                    .ofInt(binding.fabLayout, "fabBackgroundColor", fabColorFrom, fabColorTo);
                    fabAnimator.setEvaluator(argbEvaluator);

                    final AnimatorSet animatorSet = new AnimatorSet();
                    final AnimatorSet.Builder animatorSetBuilder = animatorSet.play(fabAnimator);

                    if(currentNightMode == Configuration.UI_MODE_NIGHT_NO) {
                        ObjectAnimator statusBarAnimator = ObjectAnimator.ofInt(statusBarChanger, "statusBarColor", colorFrom, colorTo);
                        statusBarAnimator.setEvaluator(argbEvaluator);
                        animatorSetBuilder.with(statusBarAnimator);
                    }

                    animatorSet.setDuration(DURATION);
                    animatorSet.start();
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

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                statusBarChanger = new StatusBarChangerLollipop();
            } else {
                statusBarChanger = new StatusBarChanger();
            }
        }

        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        }

        @Override
        public void onPageSelected(int position) {
            invalidateOptionsMenu();

            item = getItemForPosition(position);
            setItemUnread(false);

            binding.toolbarLayout.textViewSubtitle.setText(item.getFeed().getName());

            new FaviconLoader.Builder(binding.fabOpenInBrowser)
                    .withGenerateFallbackImage(false)
                    .withPlaceholder(R.drawable.ic_open_in_browser)
                    .build()
                    .load(ItemPagerActivity.this, item.getFeed(), toListener);

            progressFrom = progressTo;
            progressTo = (float) (position + 1) / (float) mSectionsPagerAdapter.getCount();
            prepareFabBar();
            binding.fabLayout.show();

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
