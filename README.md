# merge-podcast-feeds

A while ago, a friend wondered if I could help them with a
podcast-related issue. They wanted to merge a podcast feed set into a
single, unified RSS feed. One would make the seemingly valid
assumption that there would be a million and one tools out there that
do arbitrary podcast feed manipulation. After all, it's just a bit of
XML. However, multiple minutes of searching Github revealed a meager
set of candidates, none playing well with the standard podcast
formats. And so, here we are.

_merge-podcast-feeds_ does what it says on the tin and nothing
more. It merges a set of podcast feeds into a single RSS feed. You can
do one-shot merges, for those _special occations,_ or have it running
continuously as a live web service. The program supports polling and
basic WebSub and features some oddly specific Castopod integration.

A single JSON configuration file controls the program at
[`resources/json/config.json`](https://github.com/motform/merge-podcast-feeds/blob/main/resources/json/config.json). It validates feed metadata against
[iTunes' required tags](https://help.apple.com/itc/podcasts_connect/#/itcb54353390).
Individual episodes are _not_ validated as they are assumed to
originate from a correct source. Configuration errors are reported, so
check that console!

## Usage

Expects that you have a correctly formatted [`resources/json/config.json`](https://github.com/motform/merge-podcast-feeds/blob/main/resources/json/config.json), see table below for options.

Assuming you have Clojure installed, you can run the command below to check your config generate a single-shot/test .xml.

``` bash
clj -X:test
```

Once you have made sure that everything is working, you can start your server and serve the merged feed.

``` bash
clj -X:server
```

### Config

See `resources/json/config.json` for an example and starting point. The basic options are:


| key                  | value                                                                                          | required |
|----------------------|------------------------------------------------------------------------------------------------|----------|
| host-url             | _string,_ the base-url that of the feed, i.e. __foo.bar__/podcast/feed. __No trailing slashes!__ | required |
| port                 | _integer,_ the port to host the website.                                                         | required |
| slug                 | _string,_ the url foo.bar__/podcast/feed__. __Use a leading slash!__                             | required |
| castopod/base-url    | _string,_ the url of the Castopod API you are talking to. Will merge all available feeds.        | optional |
| feeds                | _string[],_ the feeds you are merging.                                                           | optional |
| logging/file-path    | _string,_ path to save the log.                                                                  | optional |
| logging/loggers      | _string[],_ logging outputs, can be `"console"`, `"file"` or both. Default `"console"`.          | optional |
| poll-rate-in-seconds | _integer,_ the poll-rate in seconds.                                                             | optional |
| websub/hub           | _string,_ the hub you want to publish to.                                                        | optional |
| xml-file-path        | _string,_ the path to save a test xml.                                                           | optional |

In addition to these, you need to set the podcast metadata. This is
the information that will be displayed in your iTunes listing, for
example. All of these keys are part of the top level `"metadata"`
object.

| key         | value                                                                   | required |
|-------------|-------------------------------------------------------------------------|----------|
| title       | _string,_ your feed title.                                                | required |
| description | _string,_ your podcast description.                                       | required |
| language    | _string,_ a [language code](https://www.rssboard.org/rss-language-codes). | required |
| atom        | _string,_ the complete feed url.                                          | required |
| copyright   | _string,_ copyright form.                                                 | optional |
| link        | _string,_ the link to your website.                                       | optional |
| image       | _object,_ see below.                                                      | optional |
| image/url   | _string,_ link to your image.                                             | required |
| image/title | _string,_ image alt code.                                                 | required |
| image/link  | _string,_ the link to you website.                                        | required |

There are a number of "iTunes" tags, see 
[the official reference](https://help.apple.com/itc/podcasts_connect/#/itcb54353390).

| key               | value                                                                                                                         | required |
|-------------------|-------------------------------------------------------------------------------------------------------------------------------|----------|
| itunes            | _object,_ see below:                                                                                                            | required |
| itunes/categories | _array of category tuples,_ see example and [iTunes list](https://podcasters.apple.com/support/1691-apple-podcasts-categories). | required |
| itunes/explicit   | _enum,_ `{"clean" "yes" "no" "true" "false"}`.                                                                                  | required |
| itunes/image      | _string,_ url to your cover art.                                                                                                | required |
| itunes/author     | _string,_ the attributed podcast author.                                                                                        | optional |
| itunes/type       | _enum,_ `{"episodic" "serial"}`.                                                                                                | optional |
| itunes/subtitle   | _string,_ short description.                                                                                                    | optional |
| itunes/summary    | _string,_ short description.                                                                                                    | optional |
| itunes/block      | _enum,_ hides the feed if set, `{"yes"}`.                                                                                       | optional |
| itunes/complete   | _enum,_ marks the feed as complete, `{"yes"}`.                                                                                  | optional |
| itunes/owner      | _object,_ see below:                                                                                                            | optional |
| owner/name        | _string,_ name of the owner.                                                                                                    | optional |
| owner/email       | _string,_ email of the owner.                                                                                                   | optional |

### Deployment 

The easiest way to deploy _merge-podcast-feeds_ is using Docke and the provided Dockerfile.

You can also build an uberjar manually with:

``` bash
clj -Sdeps '{:mvn/local-repo "./.m2/repository"}' -T:build uber
```

## Development

You can hack away code by opening an nrepl Unix socked using:

``` bash
clj -M:dev
```

## License

Copyright © 2023 [Love Lagerkvist](https://motform.org)

Distributed under the Eclipse Public License, the same as Clojure.
