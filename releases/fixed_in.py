#!/usr/bin/env python3
"""Is a commit in each Android release?

usage: python3 releases/fixed_in.py <sha> [android|core|core-legacy|resources]

Checks `git merge-base --is-ancestor <sha> <release revision>` for every release in
android-revisions.json, so the answer never depends on merge dates. The android repo is the one
this script lives in; core, core-legacy and resources are looked up as sibling checkouts
(OSMAND_REPOS env var overrides the parent folder). Run `git fetch` first if the commit is new.
"""
import json, os, subprocess, sys

HERE = os.path.dirname(os.path.abspath(__file__))
ANDROID = os.path.dirname(HERE)
PARENT = os.environ.get("OSMAND_REPOS", os.path.dirname(ANDROID))
DIRS = {
    "android": [ANDROID],
    "core": ["core", "OsmAnd-core"],
    "core-legacy": ["core-legacy", "OsmAnd-core-legacy"],
    "resources": ["resources", "OsmAnd-resources"],
}


def checkout(repo):
    for d in DIRS[repo]:
        p = d if os.path.isabs(d) else os.path.join(PARENT, d)
        if os.path.isdir(os.path.join(p, ".git")) or os.path.isfile(os.path.join(p, ".git")):
            return p
    return None


def git(path, *args):
    return subprocess.run(["git", "-C", path, *args], capture_output=True, text=True)


def main():
    if len(sys.argv) < 2:
        sys.exit(__doc__)
    sha = sys.argv[1]
    repos = [sys.argv[2]] if len(sys.argv) > 2 else list(DIRS)
    releases = json.load(open(os.path.join(HERE, "android-revisions.json")))["releases"]
    for repo in repos:
        path = checkout(repo)
        if path and git(path, "cat-file", "-e", sha + "^{commit}").returncode == 0:
            break
    else:
        sys.exit(f"{sha}: not found in {', '.join(repos)} (git fetch?)")
    info = git(path, "log", "-1", "--format=%h %ad %s", "--date=format:%d.%m.%Y %H:%M", sha).stdout.strip()
    print(f"{sha} in {repo}: {info}")
    for r in releases:
        rev = r[repo]
        found = git(path, "merge-base", "--is-ancestor", sha, rev).returncode == 0
        print(f"  {r['version']:14s} build {r['builds'][0]['start']} UTC  {repo}@{rev[:10]}  {'YES' if found else 'no'}")


if __name__ == "__main__":
    main()
