## 0.56 (2021-02-16)

* Open feed urls with OCReader (Thanks to @s3lph)

## 0.55 (2020-12-24)

* Decode received credentials after logging in (Closes: #112)
* Make folderId nullable (Closes: #113)

## 0.54 (2020-12-09)

* Support for nextcloud-news 15.1.0 (Closes: #110)

## 0.53 (2020-09-08)

* Use ViewPager instead of ViewPager2 (Closes: #108)

## 0.52 (2020-06-19)

* Override String variant of shouldOverrideUrlLoading to fix login on API < 24 (Closes: #106)
* Use primary dark color as background for action mode to make icons visible (Closes: #105)

## 0.48 (2020-06-09)

* Make feed.link nullable (Closes: #99)
* 0517944 Add breadcrumbs to preferences, remember last selected treeItem (Closes: #98)

## 0.47 (2020-06-02)

* Fix F-Droid build

## 0.46 (2020-05-28)

* Use DateUtils to format relative time span
* Use addPathSegments to build API base url (Closes: #95)
* Migrate updatedAt to lastModified (Closes: #97)

## 0.45 (2020-05-18)

* Add option to reset database
* Use nextcloud login flow to get an apptoken instead of asking for password
  * User has to login again
* Update UI
  * Use Bottomsheet instead of Drawers
* Add system setting to dark theme preference
* Rewrite in Kotlin
* Target SDK 29
* Update app icon background layer

## 0.44 (2019-01-06)

* Disable ACRA

## 0.43 (2018-05-30)

* Show warning about possible password leak

## 0.42 (2018-05-27)

* Only send stack trace and app version code when sending bug report

## 0.41 (2018-05-25)

* Disable proguard to fix GlideModule not found crash

## 0.40 (2018-05-23)

* Implemented adaptive icons, thanks to @stefan-niedermann
* Add a second article font
* Add italian translation, thanks to @airon90

## 0.39 (2018-01-20)

* Update libraries
* Use Android SDK 27

## 0.38 (2017-09-23)

* Possible fix for "You cannot start a load for a destroyed activity" crash
* Minimum SDK level raised to 17

## 0.37 (2017-08-04)

* Fallback to http when scheme is not entered and https fails when entering server details
* Add preference to change sort field
* Replace picasso with glide, which leads to better caching of favicons
* Use AboutLibraries to display libraries used
* Allow the fingerprint to be null, Fixes #69
* Use primary color as default link color in webview

## 0.36 (2017-06-05)

* Check if feed title is empty, Fixes #67
* Fix issues reported via email (realm objects no longer valid)

## 0.35 (2017-05-21)

* Show all articles when only unread is disabled (Fixes #54)
* Add french translation (Thanks to @loi219)

## 0.34 (2017-04-21)

* Update network libraries
* Save and restore webView's scrollPosition in Fragment (possible fix for #48)

## 0.33 (2017-04-18)

* Improve database migration performance

## 0.32 (2017-04-18)

* Handle items with no updatedDate

## 0.31 (2017-04-16)

* Add settings activity
* Layout improvements for article view
  * improve emoji display in articles
  * remove margin from first image in article

## 0.30 (2017-04-01)

The "This is no joke" release

* Only show unread articles when onlyUnread is selected (Fixes #49)
* Increase read timeout (Fixes #44)
* Add ACRA to report crashes (only with user's permission).
* Add play menu item in item pager when enclosure link is available

## 0.29 (2017-03-10)

* Add app shortcuts for Android 7.1
* Move buttons to mark as read and to star to the bottom in the article pager (Fixes #43)

## 0.28 (2017-02-11)

* Prepend https:// to server url if user didn't enter url scheme
* Save and restore webview's scroll position
* Fix crash when favicon uri doesn't have a path segment, Closes #42
* Save currently selected folder when pressing the back button, Fixes #40
* Support fresh articles (< 24h old) (#39), Thanks to @tobiasKaminsky

## 0.27 (2017-01-12)

* Allow user-added CAs on Android >= 7

## 0.26 (2016-12-30)

* Downgrade support libraries to 25.0.1 to fix FloatingActionButton behavior (Fixes #34)

## 0.25 (2016-12-23)

* The Floating Action Button to mark all items as read now temporarily turns into a reload button
  after marking all items as read

## 0.24 (2016-11-12)

* Fix crash when favicon failed to load
* Show feed failures in manage feeds activity

## 0.23 (2016-10-29)

* Ignore contentHash for now, fixes syncing in certain circumstances (Fixes #26)
* Add day/night theme support (Fixes #15)

## 0.22 (2016-10-23)

* Fix login with subdirectory (Thanks to @Eloo) (Fixes #23)
* Improve error messages when login failed

## 0.21 (2016-10-06)

* Fix crash when username is not known yet (Fixes #22)

## 0.20 (2016-10-06)

* Fix ordering in article view

## 0.19 (2016-10-03)

* Use the same ordering as the web interface (Fixes #19)
* Remove sorting options again, Nextcloud News with support for this is not released yet
* Workaround bug in realm 2.0.0 (realm/realm-java#3505)

## 0.18 (2016-10-02)

Note: You will have to login and sync again due to internal changes after the upgrade

* Add Russian translation (Thanks to @thfree)
* Add more sorting options (sort by publication or update date)
  * Only useful when using Nextcloud News >= 9.0.5 as the "real" publication date is not available on earlier versions (see https://github.com/nextcloud/news/pull/43)
* Fix feed icons for youtube feeds

## 0.17 (2016-09-08)

* Use the same dialog for changing and adding a feed
* Use the dominant favicon color as feed color
* Handle the case when the returned item url is NULL

## 0.16 (2016-08-26)

* Fix crash when trying to login

## 0.15 (2016-08-24)

* It's now possible to manage feeds directly in the app
    * Add new feeds via Share Dialog from other apps
* Small design improvements (inset dividers)

## 0.14 (2016-07-25)

* Only allow colors with high enough contrast as text colors for Feed titles
* Decode HTML entities in Feed titles
* Enable zoom on article views (useful for webcomics)

## 0.13 (2016-07-20)

The "upside down" release

* Use the largest available favicon
* Add ability to reverse article sort order

## 0.12 (2016-07-07)

The "Nothing to see here" release

* Fix the build on f-droid

## 0.11 (2016-06-29)

The "New colors, please" release

* Switch to a light blue color scheme (inspired by Nextcloud theme)
* Improve performance in the article view
* Add play button to play enclosed media in an external player
* Use the hash of the feed url to generate the fallback color when the feed has no favicon

## 0.10 (2016-06-11)

The "Smooth Operator" release

* Make the color transition smoother when swiping from article to article
* Add fade effect or the favicon color in the list view
* Fix favicon loading (again)
* Small design changes to better follow the material design spec
* Some fixes for sdk levels < 19

## 0.9 (2016-06-05)

The "color ALL the feeds" release

* Use Gmail-style images for feeds without favicon
* Fix initial sync not starting when logging in
* ownCloud news was renamed to Nextcloud news, rename all references
* Change color scheme (green instead of orange accent color)
* Add back the FloatingActionButton in the list view to mark all articles as read

## 0.8 (2016-06-01)

* Add action mode to article list
* Improve login activity error handling
* Don't synchronize item states in parallel
* Scroll to current item when returning from article view to the article list

## 0.7 (2016-05-22)

* Remove accepting of self-signed certs again, this undermines TLS security
* Don't start a full sync if article changes weren't synced successfully (hopefully fixes #6)
* Fixes for LoginActivity

## 0.6 (2016-05-03)

* Add possibility to accept self-signed certs (Uses [MemorizingTrustManager](https://github.com/ge0rg/MemorizingTrustManager))

## 0.5 (2016-04-29)

* Update proguard rules to fix colors not being applied in article webview

## 0.4 (2016-04-27)

* Slight UI improvement for login activity
* Treat items with the same fingerprint as one item (needs Nextcloud News >= 8.1.0)

## 0.3 (2016-03-30)

* Improve loading of feed colors in article view

## 0.2 (2016-03-17)

* Improve handling of unread/starred changes
* Fix bug leading to a crash when selecting an item in the drawer while "only Unread" is selected

## 0.1 (2016-03-07)

* Show warning when Nextcloud News is improperly configured
* Improve drawer reloading when only unread is changed

## 0.1b2 (2016-02-16)

* Make favicon loading more reliable
* Show thumbnails for embedded youtube videos
* Prevent having to scroll horizontally in article view
* Add null checks for JSON responses

## 0.1b (2016-02-10)

First beta release

* Use hardware rendering for webview on Marshmallow
* Disable toolbar scrolling in article activity
* Improve favicon handling
* Fix initial sync

## 0.1a (2016-01-31)

Initial alpha release
