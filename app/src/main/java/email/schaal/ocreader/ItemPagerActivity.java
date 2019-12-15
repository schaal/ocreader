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
import android.animation.TypeEvaluator;
import android.annotation.TargetApi;

import androidx.annotation.DrawableRes;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import androidx.databinding.DataBindingUtil;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import androidx.annotation.ColorInt;
import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.core.graphics.ColorUtils;
import androidx.viewpager.widget.ViewPager;
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

        setSupportActionBar(binding.bottomAppbar);

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
        binding.toolbarLayout.toolbar.setTitle(title);
        binding.toolbarLayout.toolbar.setSubtitle("");

        final SectionsPagerAdapter mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        binding.fabOpenInBrowser.setOnClickListener(v -> {
            if(item != null && item.getUrl() != null) {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(item.getUrl()));
                startActivity(intent);
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
            shareIntent.putExtra(Intent.EXTRA_TEXT, item.getTitle() + " - " + item.getUrl());
            startActivity(Intent.createChooser(shareIntent, getString(R.string.share_article)));
        }
    }

    public Item getItemForPosition(int position) {
        return items.get(position);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_item_pager_bottom, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.findItem(R.id.action_play_enclosure_media).setVisible(item.getEnclosureLink() != null);

        final MenuItem menuItemRead = menu.findItem(R.id.menu_mark_read);
        updateMenuItem(menuItemRead, !item.isUnread(), R.drawable.ic_check_box, R.drawable.ic_check_box_outline_blank);

        final MenuItem menuItemStarred = menu.findItem(R.id.menu_mark_starred);
        updateMenuItem(menuItemStarred, item.isStarred(), R.drawable.ic_star, R.drawable.ic_star_outline);

        return super.onPrepareOptionsMenu(menu);
    }

    private void updateMenuItem(@NonNull final MenuItem menuItem, final boolean value, @DrawableRes final int checkedIcon, @DrawableRes final int uncheckedIcon) {
        menuItem.setChecked(value);
        menuItem.setIcon(value ? checkedIcon : uncheckedIcon);
    }

    private void setItemUnread(boolean unread) {
        Queries.setItemsUnread(getRealm(), unread, this.item);
    }

    private void setItemStarred(boolean starred) {
        Queries.setItemsStarred(getRealm(), starred, this.item);
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
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case android.R.id.home:
                updateResult();
                return super.onOptionsItemSelected(menuItem);
            case R.id.action_play_enclosure_media:
                this.item.play(this);
                return true;
            case R.id.action_share_article:
                shareArticle();
                return true;
            case R.id.menu_mark_read:
                setItemUnread(!this.item.isUnread());
                updateMenuItem(menuItem, !this.item.isUnread(), R.drawable.ic_check_box, R.drawable.ic_check_box_outline_blank);
                return true;
            case R.id.menu_mark_starred:
                setItemStarred(!this.item.isStarred());
                updateMenuItem(menuItem, this.item.isStarred(), R.drawable.ic_star, R.drawable.ic_star_outline);
                return true;
            default:
                return super.onOptionsItemSelected(menuItem);
        }
    }

    private class SectionsPagerAdapter extends FragmentStatePagerAdapter {
        private final WeakHashMap<Integer, ItemPageFragment> fragments;

        SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
            fragments = new WeakHashMap<>(getCount());
        }

        @NonNull
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
            binding.bottomAppbar.setBackgroundTint(ColorStateList.valueOf(backgroundColor));
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

                    binding.fabOpenInBrowser.setBackgroundColor(fabColorTo);
                } else {
                    ObjectAnimator fabAnimator =
                            ObjectAnimator
                                    .ofInt(binding.fabOpenInBrowser, "fabBackgroundColor", fabColorFrom, fabColorTo);
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

            statusBarChanger = new StatusBarChangerLollipop();
        }

        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        }

        @Override
        public void onPageSelected(int position) {
            invalidateOptionsMenu();

            item = getItemForPosition(position);
            setItemUnread(false);

            binding.toolbarLayout.toolbar.setSubtitle(item.getFeed().getName());

            new FaviconLoader.Builder(binding.fabOpenInBrowser)
                    .withGenerateFallbackImage(false)
                    .withPlaceholder(R.drawable.ic_open_in_browser)
                    .build()
                    .load(ItemPagerActivity.this, item.getFeed(), toListener);

            progressFrom = progressTo;
            progressTo = (float) (position + 1) / (float) mSectionsPagerAdapter.getCount();

            binding.bottomAppbar.performShow();
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
