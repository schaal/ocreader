/*
 * Copyright © 2017. Daniel Schaal <daniel@schaal.email>
 *
 * This file is part of ocreader.
 *
 * ocreader is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ocreader is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Foobar.  If not, see <http://www.gnu.org/licenses/>.
 */

package email.schaal.ocreader;

import android.content.Intent;
import androidx.test.filters.LargeTest;
import androidx.test.rule.ActivityTestRule;
import androidx.test.runner.AndroidJUnit4;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;

import email.schaal.ocreader.database.model.Item;

import static androidx.test.espresso.Espresso.*;
import static androidx.test.espresso.action.ViewActions.swipeLeft;
import static androidx.test.espresso.action.ViewActions.swipeRight;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

/**
 * Created by daniel on 16.04.17.
 */

@RunWith(AndroidJUnit4.class)
@LargeTest
public class ItemPagerActivityTest {
    @Rule
    public final ActivityTestRule<ItemPagerActivity> activityTestRule = new ActivityTestRule<>(ItemPagerActivity.class, true, false);

    @Test
    public void testItemPagerActivity() throws Exception {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        ArrayList<Item> items = new ArrayList<>();
        items.add(TestGenerator.getTestItem(1));
        items.add(TestGenerator.getTestItem(2));
        intent.putExtra("ARG_ITEMS", items);
        activityTestRule.launchActivity(intent);
        onView(withId(R.id.container)).perform(swipeLeft());
        onView(withId(R.id.container)).perform(swipeRight());
    }
}
