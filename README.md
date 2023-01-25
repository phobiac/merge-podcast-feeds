# merge-podcast-feeds

You could make the seemingly valid assumption that there would be a
million and one tools out there that do arbitrary podcast feed
manipulation. However, this is apparently false, as the few tools
that exist are either incredibly specific or geared towards general
RSS manipulation that does not play well with podcast-specific tasks.

_merge-podcast-feeds_ exists because a friend needed to combine a set 
of separate podcast feeds into a single, unified feed. It does what
it says on the tin.

Podcast configuration/metadata is validated against [iTunes' required tags](https://help.apple.com/itc/podcasts_connect/#/itcb54353390).
Individual episodes are _not_ validated as they are assumed to originate
from a correct source.

## Usage

Assuming you have Clojure installed, you can run the command below to check your config generate a test .xml.

``` bash
clj -X:test-merge :config "path-to-config"
```

You can hack away code by opening an nrepl Unix socked using:

``` bash
clj -M:dev
```

## License

Copyright © 2023 [Love Lagerkvist](https://motform.org)

Distributed under the Eclipse Public License, the same as Clojure.

