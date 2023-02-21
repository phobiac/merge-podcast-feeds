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
`resources/json/config.json`. It validates feed metadata against
[iTunes' required tags](https://help.apple.com/itc/podcasts_connect/#/itcb54353390).
Individual episodes are _not_ validated as they are assumed to
originate from a correct source. Configuration errors are reported, so
check that console!

## Usage

Expects that you have a correctly formatted `resources/json/config.json`, see table below for options.

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

| host-url             | string, the base-url that of the feed, i.e. __foo.bar__/podcast/feed. __No trailing slashes!__ | required |
| port                 | integer, the port to host the website.                                                         | required |
| slug                 | string, the url foo.bar__/podcast/feed__. __Use a leading slash!__                             | required |
| castopod/base-url    | string, the url of the Castopod API you are talking to. Will merge all available feeds.        | optional |
| feeds                | string[], the feeds you are merging.                                                           | optional |
| logging/file-path    | string, path to save the log.                                                                  | optional |
| logging/loggers      | string[], logging outputs, can be `"console"`, `"file"` or both. Default `"console"`.          | optional |
| poll-rate-in-seconds | integer, the poll-rate in seconds.                                                             | optional |
| websub/hub           | string, the hub you want to publish to.                                                        | optional |
| xml-file-path        | string, the path to save a test xml.                                                           | optional |

In addition to these, you need to set the podcast metadata. This is
the information that will be displayed in your iTunes listing, for
example. All of these keys are part of the top level `"metadata"`
object.

| title           | string, your feed title.                                                | required |
| description     | string, your podcast description.                                       | required |
| language        | string, a [language code](https://www.rssboard.org/rss-language-codes). | required |
| atom            | string, the complete feed url.                                          | required |
| copyright       | string, copyright form.                                                 | optional |
| link            | string, the link to your website.                                       | optional |
| image           | object, see below.                                                      | optional |
| image/url       | string, link to your image.                                             | required |
| image/title     | string, image alt code.                                                 | required |
| image/link      | string, the link to you website.                                        | required |

There are a number of "iTunes" tags, see 
[the official reference](https://help.apple.com/itc/podcasts_connect/#/itcb54353390).

| itunes            | object, see below:                                                                                                            | required |
| itunes/categories | array of category tuples, see example and [iTunes list](https://podcasters.apple.com/support/1691-apple-podcasts-categories). | required |
| itunes/explicit   | enum, {"clean" "yes" "no" "true" "false"}.                                                                                    | required |
| itunes/image      | string, url to your cover art.                                                                                                | required |
| itunes/author     | string, the attributed podcast author.                                                                                        | optional |
| itunes/type       | enum, {"episodic" "serial"}.                                                                                                  | optional |
| itunes/subtitle   | string, short description.                                                                                                    | optional |
| itunes/summary    | string, short description.                                                                                                    | optional |
| itunes/block      | enum, hides the feed if set, {"yes"}.                                                                                         | optional |
| itunes/complete   | enum, marks the feed as complete, {"yes"}.                                                                                    | optional |
| itunes/owner      | object, see below:                                                                                                            | optional |
| owner/name        | string, name of the owner.                                                                                                    | optional |
| owner/email       | string, email of the owner.                                                                                                   | optional |

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
