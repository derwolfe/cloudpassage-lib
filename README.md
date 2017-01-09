[![Build Status](https://travis-ci.org/RackSec/cloudpassage-lib.svg?branch=master)](https://travis-ci.org/RackSec/cloudpassage-lib)
[![codecov.io](https://codecov.io/github/RackSec/cloudpassage-lib/coverage.svg?branch=master)](https://codecov.io/github/RackSec/cloudpassage-lib?branch=master)
[![Clojars Project](https://img.shields.io/clojars/v/cloudpassage-lib.svg)](https://clojars.org/cloudpassage-lib)

# cloudpassage-lib

A Clojure library for interacting with CloudPassage APIs.

For more information on the specifics of CloudPassage APIs, see the
[`cloudpassage-api.md`](doc/cloudpassage-api.md) file under `doc/`.

## API Key management

Cloudpassage issues API keys using known client-ids and client-secrets. The API
keys expire every `cloudpassage-lib.core/cache-ttl-milliseconds` seconds. All
session token's fetched via `cloudpassage-lib.core/fetch-token!` are stored in
an in-memory, thread-safe cache.

## Testing and Linting

1. `lein test` runs tests. `lein cloverage` will run tests and provide coverage
   information.

2. `lein cljfmt check` finds linting errors.

3. `lein cljfmt fix` *can* fix them, but make sure to check the output before
   committing.

## License

Copyright © 2016 Rackspace Hosting, Inc.

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
