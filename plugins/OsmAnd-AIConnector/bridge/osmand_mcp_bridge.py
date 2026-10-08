#!/usr/bin/env python3
"""Stdio MCP server that forwards to the OsmAnd AI Connector on a phone in the same Wi-Fi.

The phone announces itself as a DNS-SD service (_osmand-mcp._tcp), so the assistant config keeps no IP
address: the bridge finds the phone when it starts and again whenever the phone stops answering
(new address from the router, Wi-Fi reconnect).

The bridge answers initialize and tools/list itself when the phone is not reachable, from a cache of the
last successful connect, so the assistant session starts with the OsmAnd tools even while the phone sleeps;
a tool call then tells the assistant that the phone is not found.

    claude mcp add osmand -- python3 /path/to/osmand_mcp_bridge.py --token <access key>

Needs only Python 3 and dns-sd (macOS) or avahi-browse (Linux).
"""
import argparse
import http.client
import json
import os
import re
import shutil
import socket
import subprocess
import sys
import urllib.parse

SERVICE_TYPE = "_osmand-mcp._tcp"
BROWSE_SECONDS = 3.0
CONNECT_SECONDS = 3.0
CACHE_FILE = os.path.expanduser("~/.osmand/osmand_mcp_cache.json")
PROTOCOL_VERSIONS = ["2025-06-18", "2025-03-26", "2024-11-05"]
PHONE_NOT_FOUND = ("The phone with OsmAnd is not reachable: {}. Ask the user to turn on OsmAnd AI Connector "
                   "in Wi-Fi mode on a phone in the same network, then call the tool again.")


def log(msg):
    print(f"osmand-bridge: {msg}", file=sys.stderr, flush=True)


def unescape(name):
    # dns-sd writes "\032" for a space and "\." for a dot
    return re.sub(r"\\(\d{3}|.)", lambda m: chr(int(m.group(1))) if m.group(1).isdigit() else m.group(1), name)


def browse_dns_sd():
    """[(name, host, port, txt)] from `dns-sd -Z`, which prints SRV and TXT records of every instance."""
    proc = subprocess.Popen(["dns-sd", "-Z", SERVICE_TYPE, "local"], stdout=subprocess.PIPE,
                            stderr=subprocess.DEVNULL, text=True)
    try:
        out, _ = proc.communicate(timeout=BROWSE_SECONDS)
    except subprocess.TimeoutExpired:
        proc.kill()
        out, _ = proc.communicate()
    found = {}
    for line in out.splitlines():
        parts = line.split(None, 2)
        if len(parts) < 3 or not parts[0].endswith("." + SERVICE_TYPE):
            continue
        name = unescape(parts[0][:-len(SERVICE_TYPE) - 1])
        entry = found.setdefault(name, {"txt": {}})
        if parts[1] == "SRV":
            srv = parts[2].split()
            entry["port"], entry["host"] = int(srv[2]), srv[3].rstrip(".")
        elif parts[1] == "TXT":
            for kv in re.findall(r'"([^"]*)"', parts[2]):
                k, _, v = kv.partition("=")
                entry["txt"][k] = v
    return [(n, e["host"], e["port"], e["txt"]) for n, e in found.items() if "host" in e]


def browse_avahi():
    out = subprocess.run(["avahi-browse", "-rpt", SERVICE_TYPE], capture_output=True, text=True,
                         timeout=BROWSE_SECONDS + 5).stdout
    found = []
    for line in out.splitlines():
        f = line.split(";")
        # =;iface;IPv4;name;type;domain;host;address;port;"k=v" "k=v"
        if len(f) >= 10 and f[0] == "=" and f[2] == "IPv4":
            txt = dict(kv.partition("=")[::2] for kv in re.findall(r'"([^"]*)"', f[9]))
            found.append((unescape(f[3]), f[7], int(f[8]), txt))
    return found


def discover(name_filter):
    if shutil.which("dns-sd"):
        services = browse_dns_sd()
    elif shutil.which("avahi-browse"):
        services = browse_avahi()
    else:
        raise RuntimeError("neither dns-sd nor avahi-browse is installed; pass --url instead")
    if name_filter:
        services = [s for s in services if name_filter.lower() in s[0].lower()]
    if not services:
        raise RuntimeError("no OsmAnd AI Connector found on this network; is the phone on the same Wi-Fi "
                           "and the connector on in Wi-Fi mode?")
    # an old announcement or an address this computer cannot reach is skipped
    for name, host, port, txt in services:
        try:
            addr = socket.getaddrinfo(host, port, socket.AF_INET, socket.SOCK_STREAM)[0][4][0]
            socket.create_connection((addr, port), CONNECT_SECONDS).close()
        except OSError as e:
            log(f"'{name}' at {host}:{port} is not reachable: {e}")
            continue
        url = f"http://{addr}:{port}{txt.get('path') or '/mcp'}"
        log(f"found '{name}' at {url}")
        return url
    raise RuntimeError("the OsmAnd AI Connector was found but does not answer: " + ", ".join(s[0] for s in services))


class Unreachable(Exception):
    """The request did not reach the phone."""


class HttpError(Exception):
    pass


class Bridge:

    def __init__(self, args):
        self.args = args
        self.url = args.url
        self.cache = self.load_cache()
        self.tools_from_cache = False

    def endpoint(self, rediscover=False):
        if self.args.url:
            return self.args.url
        if rediscover or not self.url:
            self.url = discover(self.args.name)
        return self.url

    def post(self, url, body):
        u = urllib.parse.urlsplit(url)
        # a short connect timeout: an old address must not hang the assistant for the whole tool timeout
        conn = http.client.HTTPConnection(u.hostname, u.port or 80, timeout=CONNECT_SECONDS)
        try:
            try:
                conn.connect()
            except OSError as e:
                raise Unreachable(e)
            conn.sock.settimeout(self.args.timeout)
            conn.request("POST", u.path or "/", body=body, headers={
                "Content-Type": "application/json",
                "Accept": "application/json, text/event-stream",
                "Authorization": f"Bearer {self.args.token}",
            })
            resp = conn.getresponse()
            data = resp.read()
            if resp.status >= 400:
                raise HttpError(f"HTTP {resp.status} {data.decode('utf-8', 'replace').strip()}")
            return data
        finally:
            conn.close()

    def forward(self, body):
        try:
            return self.post(self.endpoint(), body)
        except Unreachable as e:
            # nothing reached the phone, so repeating the request cannot run a tool twice
            if self.args.url:
                raise
            log(f"phone does not answer ({e}), looking for it again")
            return self.post(self.endpoint(rediscover=True), body)

    def call(self, msg):
        """Forwards one request and returns the parsed reply; raises when the phone is not reachable."""
        out = self.forward(json.dumps(msg).encode("utf-8"))
        return json.loads(out) if out and out.strip() else None

    def handle(self, msg):
        method, rid = msg.get("method"), msg.get("id")
        if rid is None:
            # notifications: the connector ignores them, so an offline phone must not delay the session
            return None
        if method == "initialize":
            try:
                reply = self.call(msg)
                if "result" in reply:
                    self.save_cache(initialize=reply["result"])
            except Exception as e:
                log(f"phone not reachable at start ({e}), answering initialize from the cache")
                reply = {"jsonrpc": "2.0", "id": rid, "result": self.local_initialize(msg)}
            # the bridge tells the assistant when the cached tools turn out to be outdated
            reply.get("result", {}).setdefault("capabilities", {})["tools"] = {"listChanged": True}
            return reply
        if method == "tools/list":
            try:
                reply = self.call(msg)
                if "result" in reply:
                    self.save_cache(tools=reply["result"].get("tools"))
                    self.tools_from_cache = False
                return reply
            except Exception as e:
                tools = self.cache.get("tools")
                if tools is None:
                    raise RuntimeError(f"the phone is not reachable ({e}) and no tools are cached yet; "
                                       "connect once with the phone on")
                log(f"phone not reachable ({e}), {len(tools)} tools from the cache")
                self.tools_from_cache = True
                return {"jsonrpc": "2.0", "id": rid, "result": {"tools": tools}}
        if method == "tools/call":
            try:
                reply = self.call(msg)
            except Exception as e:
                return {"jsonrpc": "2.0", "id": rid, "result": {
                    "content": [{"type": "text", "text": PHONE_NOT_FOUND.format(e)}], "isError": True}}
            return reply
        return self.call(msg)

    def local_initialize(self, msg):
        asked = (msg.get("params") or {}).get("protocolVersion")
        cached = self.cache.get("initialize") or {}
        return {
            "protocolVersion": asked if asked in PROTOCOL_VERSIONS else PROTOCOL_VERSIONS[0],
            "capabilities": {"tools": {"listChanged": True}},
            "serverInfo": cached.get("serverInfo") or {"name": "osmand-ai-connector", "version": "bridge"},
            "instructions": cached.get("instructions") or "Controls the OsmAnd map app on an Android phone.",
        }

    def check_tools(self):
        """After the phone answers, replaces tools served from an outdated cache."""
        if not self.tools_from_cache:
            return
        self.tools_from_cache = False
        try:
            reply = self.call({"jsonrpc": "2.0", "id": "bridge-tools", "method": "tools/list"})
            tools = reply["result"]["tools"]
        except Exception as e:
            log(f"cannot refresh the tools: {e}")
            return
        if tools != self.cache.get("tools"):
            self.save_cache(tools=tools)
            log("the phone has other tools than the cache, telling the assistant")
            write({"jsonrpc": "2.0", "method": "notifications/tools/list_changed"})

    def load_cache(self):
        try:
            with open(CACHE_FILE) as f:
                return json.load(f)
        except (OSError, ValueError):
            return {}

    def save_cache(self, **values):
        self.cache.update(values)
        try:
            os.makedirs(os.path.dirname(CACHE_FILE), exist_ok=True)
            with open(CACHE_FILE, "w") as f:
                json.dump(self.cache, f)
        except OSError as e:
            log(f"cannot save {CACHE_FILE}: {e}")

    def run(self):
        for line in sys.stdin:
            line = line.strip()
            if not line:
                continue
            try:
                msg = json.loads(line)
            except ValueError:
                continue
            if not isinstance(msg, dict):
                continue
            try:
                reply = self.handle(msg)
            except Exception as e:
                reply = None
                if msg.get("id") is not None:
                    reply_error(msg["id"], f"OsmAnd AI Connector: {e}")
            if reply is not None:
                write(reply)
                if msg.get("method") == "tools/call" and "result" in reply and not reply["result"].get("isError"):
                    # the phone answers again: replace tools served from an outdated cache
                    self.check_tools()


def write(msg):
    sys.stdout.write(json.dumps(msg) + "\n")
    sys.stdout.flush()


def reply_error(rid, message):
    write({"jsonrpc": "2.0", "id": rid, "error": {"code": -32000, "message": message}})


def main():
    p = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    p.add_argument("--token", default=os.environ.get("OSMAND_MCP_TOKEN"),
                   help="access key shown in the connector app (or OSMAND_MCP_TOKEN)")
    p.add_argument("--name", help="part of the phone name when several phones run the connector")
    p.add_argument("--url", help="fixed server URL, no discovery (e.g. http://127.0.0.1:8765/mcp over adb)")
    p.add_argument("--timeout", type=float, default=120, help="seconds to wait for one tool call")
    p.add_argument("--list", action="store_true", help="print the connectors found on this network and exit")
    args = p.parse_args()
    if args.list:
        for name, host, port, txt in (browse_dns_sd() if shutil.which("dns-sd") else browse_avahi()):
            print(f"{name}\t{host}:{port}\t{txt}")
        return
    if not args.token:
        p.error("--token or OSMAND_MCP_TOKEN is required")
    Bridge(args).run()


if __name__ == "__main__":
    main()
