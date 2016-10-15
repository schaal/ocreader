/*
 * Copyright (C) 2016 Daniel Schaal <daniel@schaal.email>
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

import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.RecordedRequest;

/**
 * Created by daniel on 15.10.16.
 */

class APIDispatcher extends Dispatcher {
    @Override
    public MockResponse dispatch(RecordedRequest request) throws InterruptedException {
        String PATH_PREFIX = "/index.php/apps/news/api";
        if(request.getPath().equals(PATH_PREFIX))
            return new MockResponse().setBody("{ \"apiLevels\": [\"v1-2\"]}");
        else if(request.getPath().equals(PATH_PREFIX + "/v1-2/status"))
            return new MockResponse().setBody("{ \"version\": \"8.8.0\", \"warnings\": { \"improperlyConfiguredCron\": false }}");
        return new MockResponse().setResponseCode(404);
    }
}
