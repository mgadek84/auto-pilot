# Selenium Recruitment Tasks

UI test automation for the four recruitment tasks, written with **Selenium WebDriver** and
**TestNG** on **Java 1.8** and built with **Maven**.

The suite drives real websites (Wikipedia, Google, W3Schools, YouTube). Every wait is an
explicit `WebDriverWait` bound to a condition - there is no `Thread.sleep` in the project.

## What the tasks do

| Test class | Task |
| --- | --- |
| `com.gitlab.rmarzec.task.Task1Test` | Verifies the toolchain: the sources are compiled for Java 1.8, the tests run under Maven and a browser session can be started. |
| `com.gitlab.rmarzec.task.Task2Test` | Opens `https://pl.wikipedia.org/wiki/Wiki`, opens the language selector, collects every language as a `List<WebElement>`, prints each language name and prints the `href` of the English version. |
| `com.gitlab.rmarzec.task.Task3Test` | Searches Google for `HTML select tag - W3Schools`, uses "Szczęśliwy traf" (I'm Feeling Lucky), verifies (and corrects) the landing URL, opens the first "Try it Yourself" editor, switches to the `iframeResult` frame, prints the "The select element" header and selects `Opel` with Selenium's `Select` class, printing `Opel, opel`. |
| `com.gitlab.rmarzec.task.Task4Test` | Opens YouTube, accepts consent, visits Shorts and prints the channel of the playing Short, returns home, searches for `Live`, collects the first 12 standard video results into a `List<YTTile>` (title, channel, duration - `live` for streams) and prints all results that are not live. |

## Requirements

- **JDK 1.8** (the build targets `1.8`; it also compiles and runs on newer JDKs)
- **Maven 3.6+**
- **Google Chrome** (default) or **Firefox**

The browser driver is resolved automatically by Selenium Manager, which ships with Selenium 4.
Nothing has to be downloaded by hand. On machines without internet access to the driver CDN,
point the tests at a local driver instead:

```bash
mvn test -Dwebdriver.chrome.driver=/path/to/chromedriver
```

## Running the tests

Run the whole suite (`src/test/resources/suites/all-tasks.xml`):

```bash
mvn clean test
```

Run a single task:

```bash
mvn test -Dtest=Task3Test
mvn test -Dtest=Task4Test#collectsFirstTwelveVideoResultsForLiveKeyword
```

Watch the browser instead of running headless:

```bash
mvn test -Dheadless=false
```

### Runtime switches

| Property | Default | Meaning |
| --- | --- | --- |
| `browser` | `chrome` | `chrome` or `firefox` |
| `headless` | `true` | Run the browser without a window |
| `explicitWaitSeconds` | `25` | Timeout of every `WebDriverWait` |
| `pageLoadTimeoutSeconds` | `60` | Page load timeout |
| `browserLanguage` | `pl-PL` | Requested browser UI language |
| `suiteFile` | `src/test/resources/suites/all-tasks.xml` | TestNG suite to execute |
| `webdriver.chrome.driver` | *(unset)* | Pin a local chromedriver binary |

## Project layout

```
src/test/java/com/gitlab/rmarzec/
├── core/      framework: configuration, driver factory, base test, base page, reporting
├── model/     YTTile - data of a single YouTube search result
├── pages/     page objects: Wikipedia, Google, W3Schools, YouTube
├── support/   cookie consent handling shared by the page objects
└── task/      Task1Test .. Task4Test - the tasks themselves
src/test/resources/suites/all-tasks.xml
```

### Design notes

- **Page objects** own the locators and the interactions; the test classes only express the
  steps of a task, its console output and its assertions.
- **Resilient locators.** Sites in this suite ship several UI generations at once (Wikipedia
  Vector 2022 vs. legacy skins and the Universal Language Selector dialog, classic
  `ytd-video-renderer` vs. the newer `yt-lockup-view-model` on YouTube). Page objects declare
  ordered locator candidates and use the first one that matches, so a UI change on one
  generation does not break the test.
- **Clicks that reach the target.** `BasePage#click` tries a WebDriver click, then a mouse
  click at the element position (needed for controls painted under an overlay, such as the
  Wikipedia language dropdown), then a scripted click.
- **`StaleElementReferenceException`** is ignored by every wait, element reads retry once, and
  lookups return empty results instead of throwing when a container is re-rendered. On the
  YouTube feed, a result tile that is still rendering is re-read on the next pass rather than
  reported half filled.
- **Optional UI** (cookie banners, skin-specific buttons) is handled with short bounded waits
  and never fails a test when it is absent.
- **Failure diagnostics.** `ScreenshotOnFailureListener` stores a PNG of the browser and logs
  the current URL to `target/screenshots` for every failed test.

## Notes on the live sites

- Google shows the "Szczęśliwy traf" button only on the classic home page layout and its
  redirect can land anywhere. Task 3 logs the URL it landed on and navigates to
  `https://www.w3schools.com/tags/tag_select.asp` explicitly when needed, exactly as the task
  requires.
- YouTube localises its UI by IP, so the consent, Shorts and result locators cover both the
  Polish and the English wording.
- YouTube regularly refuses to *play* video in an automated browser session ("Video
  unavailable") while still rendering the reel metadata. Task 4 therefore identifies the
  current Short by the video id in the address bar, reads the channel from that reel, and logs
  when the player reported the video as unplayable - instead of silently reporting a channel
  taken from a neighbouring reel.
- YouTube renders results in batches; the results page scrolls until 12 standard videos
  (no Shorts, playlists, mixes or ads) are available.
