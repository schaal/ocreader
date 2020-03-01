/*
 * Copyright 2018 Realm Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package email.schaal.ocreader.database

import androidx.annotation.MainThread
import androidx.lifecycle.MutableLiveData
import io.realm.OrderedRealmCollectionChangeListener
import io.realm.RealmModel
import io.realm.RealmResults

/**
 * This class represents a RealmResults wrapped inside a LiveData.
 *
 * Realm will always keep the RealmResults up-to-date whenever a change occurs on any thread,
 * and when that happens, the observer will be notified.
 *
 * The RealmResults will be observed until it is invalidated - meaning all local Realm instances on this thread are closed.
 *
 * @param <T> the type of the RealmModel
</T> */
class LiveRealmResults<T : RealmModel> @MainThread constructor(results: RealmResults<T>) : MutableLiveData<List<T>>() {
    private val results: RealmResults<T>
    // The listener will notify the observers whenever a change occurs.
// The results are modified in change. This could be expanded to also return the change set in a pair.
    private val listener = OrderedRealmCollectionChangeListener<RealmResults<T>> { results, _ -> this@LiveRealmResults.value = results }
    // We should start observing and stop observing, depending on whether we have observers.
    /**
     * Starts observing the RealmResults, if it is still valid.
     */
    override fun onActive() {
        super.onActive()
        if (results.isValid) { // invalidated results can no longer be observed.
            results.addChangeListener(listener)
            value = results
        }
    }

    /**
     * Stops observing the RealmResults.
     */
    override fun onInactive() {
        super.onInactive()
        if (results.isValid) {
            results.removeChangeListener(listener)
        }
    }

    init {
        require(results.isValid) { "The provided RealmResults is no longer valid, the Realm instance it belongs to is closed. It can no longer be observed for changes." }
        this.results = results
        if (results.isLoaded) {
            // we should not notify observers when results aren't ready yet (async query).
            // however, synchronous query should be set explicitly.
            value = results
        }
    }
}