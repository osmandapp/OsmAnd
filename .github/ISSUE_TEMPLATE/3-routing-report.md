---
name: "🚩 Routing report"
about: Report a routing (and/or GPX track) problem in OsmAnd
title: 
labels: 
---

<!--🔅🔅🔅🔅🔅🔅🔅🔅🔅🔅🔅🔅🔅🔅🔅🔅🔅🔅🔅🔅🔅🔅🔅🔅🔅🔅🔅🔅🔅🔅🔅🔅

🤚 👉 READ ME FIRST! 👈

Oh hi there! 😄

Before submitting a new issue, please:
* search for existing (open AND/OR closed) issues
* check existing [Discussions](/osmandapp/OsmAnd/discussions)
* be sure to check the [help information](//osmand.net/help-online).

Existing issues often contain information about workarounds, resolution, or progress updates. For a short & humourous (but entirely relevant) aside, watch [Posting and You…](//www.youtube.com/watch?v=B9q2jNjOPdk).

Because there are hundreds of requests each month and a much smaller number of developers, checking other resources *first* helps developers to use their time efficiently (which includes issue-handling, like triage). This enables developers to spend more time doing actual software development (which includes examining your report & determining a solution), instead of project management. That helps everyone. Developers are human, with their own lives, demands on their time, stress, and so on.

Since GitHub is the main development centre for OsmAnd, each new issue (including yours) will be sent to all developers. However, please know that busy developers might simply close your report with only a brief message (or sometimes none, especially if it's previously been reported and/or recently fixed).
Comments on closed issues are also sent to all developers, so you will definitely be heard.
However, there is no guarantee that a developer will pick up the issue to work on it. Developers work on many parts of OsmAnd, and must prioritise their time.
Patience is strongly advised. Your report may be complicated, and take more time to address; not receiving an immediate response doesn't mean that it's being ignored.
Donating (what you can afford) wouldn't go amiss, either. Developers have living expenses, so more funding might mean more developers.
In the meantime, you may receive suggestions or questions from enthusiastic users (many of whom are knowledgeable). Sometimes the root-cause of a problem isn't with OsmAnd itself, but something else which it relies on. Enthusiastic users may try to determine this, or to help narrow down the cause of the problem (for developers to have an earlier (quicker) task).

With all that in mind; the best way to help developers to help *you* (with your report) is to carefully craft a valid, descriptive and detailed report. For more information, read [How to Report Bugs Effectively](//www.chiark.greenend.org.uk/~sgtatham/bugs.html).

Title: please use a clear (descriptive, succinct) title for your report. For guidance, read [Microcontent: How to Write Headlines, Page Titles, and Subject Lines](//www.nngroup.com/articles/microcontent-how-to-write-headlines-page-titles-and-subject-lines/).

Please provide the following information (at minimum; more is OK) so that we can try to **reproduce** your issue, in order to understand it, to know how to fix it:

🔅🔅🔅🔅🔅🔅🔅🔅🔅🔅🔅🔅🔅🔅🔅🔅🔅🔅🔅🔅🔅🔅🔅🔅🔅🔅🔅🔅🔅🔅🔅🔅-->

## 🚩 Routing report

### Background & preparation
#### ☑ Checking existing resources (‘homework’)
<!-- tick applicable boxes (like this: [x])) -->
* [ ] 🔎 I **searched** for, and **read**, [existing Issues (including closed)](/osmandapp/OsmAnd/issues?q=is:issue+) for similar reports
* [ ] 🔎 I **searched** for, and **read**, [Discussions](/osmandapp/OsmAnd/discussions) <!--[future:](including closed)[see: http://github.com/github/feedback/discussions/3097]--> for similar problems
* [ ] 👓 I **read** the FAQ & relevant [help](//osmand.net/help-online)
#### Doing my part; helping you to help me
<!-- tick applicable boxes (like this: [x])) -->
* [ ] 👓 I read & **followed the instructions** for completing this report
* [ ] ⛔ I'm **not** expecting OsmAnd to do anything impossible / forbidden, such as to drive through a pedestrian area, walk across open water, or make an illegal turn
* [ ] ❓ I'll respond to **questions**, and answer them as best I can to help developers

---

### 🧮 Routing engine

<!-- Which routing provider was used? (please tick the applicable box (like this: [x])) -->

- [ ] OsmAnd's **in-app offline** routing
- [ ] Any **online** routing provider (YOURS, openrouteservice, OSRM, etc.) <!--  -->
- [ ] **BRouter** (companion app) <!--  -->

### Routing profile

#### 🚙 Vehicle
<!-- What routing profile was chosen in the OsmAnd app? (please tick the applicable box (like this: [x])) -->
* [ ] 🚗 **motor vehicle** (e.g. car)
* [ ] 🚲 **bicycle** (foot-pedal bike, not motorcycle)
* [ ] 🥾 **walking** (pedestrian, hiking, or otherwise **on-foot**)
* [ ] 🚌 public transport (**bus**, **train**)
* [ ] 🎿 skiing / snowboarding
* [ ] off-road / 4×4
* [ ] ✈ **flying** (aircraft or otherwise **airborne**)
* [ ] 🛥 **marine** (boat or otherwise on **water**)
* [ ] other (please specify)

#### Mode
<!-- Which routing-finding mode was chosen in the OsmAnd app? (please tick the applicable box (like this: [x])) -->
* [ ] ⏩ **quickest** route
* [ ] ⛽ **fuel-saving** (efficiency) route
* [ ] **shortest** route
* [ ] I'm using a **GPX** track (recorded track or planned route) <!-- please provide (a link to) the GPX track you're using -->

### ↔️ Route scope (start and end points)

<!-- please tick the applicable box (like this: [x]) -->

I'm providing <!-- further below -->
* [ ] **entire** route <!-- be sure to clearly describe (✍) where the troublesome part is, within the whole -->
* [ ] the **troublesome part** of the route
<!-- feel free to include both the overall route and (separately) the troublesome part -->

### 🏁 Destination selection method

<!-- please tick the applicable box (like this: [x]) -->

* [ ] ✒ typing a (postal) **address** <!-- note that this requires a (Nominatim) lookup for the coördinates, which may not be as optimal as manual selection -->
* [ ] 🔍 **search**ing for a **point-of-interest** by type
* [ ] 🗺 tapping the destination on the **map**
* [ ] 🔗 following a **link** from another app

<!-- If inputting an address, please include (just below) the exact address you typed into OsmAnd -->
<!--✍️-->

### 🛣 Suggested and expected routes

<!-- Tell us what route OsmAnd suggested and describe (or, better, show) how your expected route differs.
Since developers are unlikely to be familiar with the region in question, please make every effort to be clear and make it easy to understand the route (both expected & divergent), without expecting local knowledge or study of the map. It should be clear at a glance, what the problem is.
Adding screenshots (preferably orientated **North-up**, to match online maps) will help a lot.

Please specify (as exactly as possible) your route:
* the clearest, most reliable method is by giving coördinates
* to generate URLs (Web addresses) for filling in the below templates, the easiest way would be to visit each site, input your origin & destination for the site to calculate the route, then copy (or ‘share’) the URL from your browser
* linking to the relevant element on [openstreetmap.org](//www.openstreetmap.org/) is often helpful
-->

<!-- template: -->
* [My route via `OSM.org`](//www.openstreetmap.org/directions?route=0.0%2C0.0%3B90.0%2C0.0)
* [My route via `OsmAnd.net/map`](//www.osmand.net/map/?start=0%2C0&end=90%2C0)

<!-- If you need to show other examples, list them here (copy & paste the above template) -->
<!--✍️-->

This route is unexpected in the following ways:
* <!-- screenshot / link / ✍️ detailed description -->
* <!-- screenshot / link / ✍️ detailed description -->

---

### 🗺 OSM map data

<!-- Often, the cause of poor routing is inaccurate map data (e.g. ways & tagging); please tick the applicable box (like this: [x]) -->

<!-- tick the applicable box (like this: [x]) -->
* [ ] I have **not** checked relevant [OSM map data](//www.openstreetmap.org/)
* [ ] I have checked relevant [OSM map data](//www.openstreetmap.org/), and **found problems**
* [ ] I have checked relevant [OSM map data](//www.openstreetmap.org/), and **didn't find problems**

If having checked, which elements <!-- include a permalink to each --> are relevant or suspicious:

* <!-- example: [name/ID of road](//www.openstreetmap.org/way/1) -->
* <!-- example: [name/ID of intersection](//www.openstreetmap.org/relation/1) -->
* <!-- example: [name/ID of junction](//www.openstreetmap.org/node/1) -->

<!-- tick the applicable box (like this: [x]) -->
* [ ] I *have* **fixed** problems in the relevant map data
* [ ] I have **not** attempted to fix any problems in the relevant map data

---

### 🔙 Is this a regression?

<!-- Did the expected routing work in a previous version, but not in a newer version? -->

* [ ] **No**, same in both
* [ ] **Yes**, they're different
* [ ] I **don't know** 🤷‍♂️

If yes, the previous version (of OsmAnd) in which this bug was not present is: <!--✍️--> vN.N.NN

---

### 📱 Your Environment

#### Software versions
##### OsmAnd version
OsmAnd <!--✍️--> vN.N.NN <!-- you can find your version number in the Help, About area of the app -->

##### Device OS
<!-- tick applicable box (like this: [x]) -->
* [ ] **Android** (or compatible) <!-- If using a compatible non-Google distro, please specify which -->
* [ ] Apple **iOS**

###### Device OS version
<!--✍-->

#### Maps used

<!-- Please tick applicable box(es) (like this: [x]) -->

- [ ] **Offline map**s offered within the OsmAnd app for download
- [ ] **Online (tile / raster) map**s <!-- Please name it -->

I'm using these specific maps
<!-- If you are using offline maps, tell us the exact name of the map file where the issue occurs and its edition date. A list can be found at: https://download.osmand.net/list.php -->
* <!--✍️-->
* <!--✍️-->

---

### Other relevant information
<!--✍️-->

### Suggestions
<!-- Do you have any thoughts on how the routing might be improved? If so, list them below. -->
<!--✍️-->


<!-- be sure to preview your report, and fix any problems, before submitting it -->
