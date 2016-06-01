## 0.8 (2016-06-01)

* Add action mode to article list
* Improve login activity error handling
* Don't synchronize item states in parallel
* Scroll to current item when returning from article view to the article list

## 0.7 (2016-05-22)

* Remove accepting of self-signed certs again, this undermines TLS security (see e.g. https://github.com/owncloud/news/blob/master/docs/developer/External-Api.md#security-guidelines)
* Don't start a full sync if article changes weren't synced successfully (hopefully fixes #6)
* Fixes for LoginActivity

## 0.6 (2016-05-03)

* Add possibility to accept self-signed certs (Uses [MemorizingTrustManager](https://github.com/ge0rg/MemorizingTrustManager))

## 0.5 (2016-04-29)

* Update proguard rules to fix colors not being applied in article webview

## 0.4 (2016-04-27)

* Slight UI improvement for login activity
* Treat items with the same fingerprint as one item (needs ownCloud news >= 8.1.0)

## 0.3 (2016-03-30)

* Improve loading of feed colors in article view

## 0.2 (2016-03-17)

* Improve handling of unread/starred changes
* Fix bug leading to a crash when selecting an item in the drawer while "only Unread" is selected

## 0.1 (2016-03-07)

* Show warning when ownCloud news is improperly configured
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
