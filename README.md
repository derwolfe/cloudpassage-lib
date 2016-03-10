[![Build Status](https://travis-ci.org/RackSec/cloudpassage-lib.svg?branch=master)](https://travis-ci.org/RackSec/cloudpassage-lib)
[![codecov.io](https://codecov.io/github/RackSec/cloudpassage-lib/coverage.svg?branch=master)](https://codecov.io/github/RackSec/cloudpassage-lib?branch=master)

# cloudpassage-lib

A Clojure library for interacting with CloudPassage APIs.

For more information on the specifics of CloudPassage APIs, see the
[`cloudpassage-api.md`](doc/cloudpassage-api.md) file under `doc/`.

## Setup

1. Install or have a running, reachable instance of redis available.

2. Install Leiningen, if you don't already have it, and run

   ```
   lein deps
   ```

   to fetch dependencies.

3. Obtain a fernet key. You only need to generate this once; you can store it
   in your `.lein-env` later, per step 2.

   ```clojure
   lein repl
   cloudpassage-lib.core=> (require 'fernet.core)
   nil
   cloudpassage-lib.core=> (fernet.core/generate-key)
   "9HS-DrHsi48OBk51jDcAwiHT6Xy5GPqjtnB4guVWpp0"
   ```

4. Set the following environment variables. One could use a profile or
   `.lein-env` file instead.

   ```bash
   # Set some shell environment variables
   REDIS_URL=redis://localhost:6379
   REDIS_TIMEOUT=4000
   FERNET_KEY=AVALIDFERNETKEY
   ```

   ```clojure
   ; Paste this into a .lein-env file in the project root
   {:redis-url "redis://localhost:6379"
    :redis-timeout 4000
    :fernet-key "AVALIDFERNETKEY"}
   ```

## API Key management

Cloudpassage issues API keys using known client-ids and client-secrets. The API
keys expire every 900 seconds. We store each account's API key in Redis under
the key `account-<client-id>`.

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
