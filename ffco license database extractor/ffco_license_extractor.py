import json
import re
import time
import urllib.parse
import urllib.request
from dataclasses import dataclass
from typing import Dict, List, Optional, Tuple


BASE_URL = "https://inscriptions.co-paca.info"
MAIN_URL = f"{BASE_URL}/"


@dataclass(frozen=True)
class Club:
    club_id: str
    club_name: str


@dataclass(frozen=True)
class Runner:
    runnerName: str
    clubName: str
    sportidentId: int
    category: str
    licenseId: int


class RateLimitedHttpClient:
    def __init__(self, min_interval_seconds: float = 1.0, user_agent: str = "FFCO-Extractor/1.0"):
        self._min_interval_seconds = float(min_interval_seconds)
        self._user_agent = user_agent
        self._last_request_ts: Optional[float] = None

    def _sleep_if_needed(self) -> None:
        if self._last_request_ts is None:
            return
        elapsed = time.time() - self._last_request_ts
        remaining = self._min_interval_seconds - elapsed
        if remaining > 0:
            time.sleep(remaining)

    def get_text(self, url: str, timeout_seconds: int = 20) -> str:
        self._sleep_if_needed()
        print(f"[DEBUG] GET {url}")
        req = urllib.request.Request(url, headers={"User-Agent": self._user_agent}, method="GET")
        with urllib.request.urlopen(req, timeout=timeout_seconds) as resp:
            raw = resp.read()
        self._last_request_ts = time.time()
        return raw.decode("utf-8", errors="replace")

    def post_form_text(self, url: str, form: Dict[str, str], timeout_seconds: int = 20) -> str:
        self._sleep_if_needed()
        body = urllib.parse.urlencode(form).encode("utf-8")
        print(f"[DEBUG] POST {url} form={form}")
        req = urllib.request.Request(
            url,
            data=body,
            headers={
                "User-Agent": self._user_agent,
                "Content-Type": "application/x-www-form-urlencoded",
            },
            method="POST",
        )
        with urllib.request.urlopen(req, timeout=timeout_seconds) as resp:
            raw = resp.read()
        self._last_request_ts = time.time()
        return raw.decode("utf-8", errors="replace")


def find_highest_competition_id(main_html: str) -> int:
    # href="https://inscriptions.co-paca.info/inscrit_club.php?no={competitionId}"
    regex = re.compile(r'href="inscription1\.php\?no=(\d+)"')
    ids = [int(m.group(1)) for m in regex.finditer(main_html)]
    print("[DEBUG] Found competitionIds: ", ids)
    if not ids:
        raise RuntimeError("Unable to find any competitionId on the main page.")
    return max(ids)


def parse_clubs(clubs_html: str) -> List[Club]:
    print("[DEBUG] Parsing clubs from HTML")
    print(f"{clubs_html}")
    # <span class="html-attribute-value">{clubId}</span> &gt;</span>{clubName}<span class="html-tag">
    # <option value="503" >503 BNCO </option>
    # <option value=503 >503 BNCO </option>
    regex = re.compile( r'<option[^>]*\bvalue\s*=\s*(?:"?(\d+)"?)[^>]*>\s*\d+\s+([^<]+?)\s*</option>',
    re.IGNORECASE,)

    clubs: List[Club] = []
    for m in regex.finditer(clubs_html):
        club_id = m.group(1).strip()
        club_name = m.group(2).strip()
        if club_id and club_name:
            clubs.append(Club(club_id=club_id, club_name=club_name))

    print(f"[DEBUG] Clubs: {clubs}")
    if not clubs:
        raise RuntimeError("No clubs found in the competition page.")
    return clubs


def parse_runners(runners_html: str, club_name: str) -> List[Runner]:
    # &gt;</span>{runnerName} | {sportidentId} | H45<span class="html-tag">&lt;/option&gt;
    print(f"[DEBUG] Parsing runners for club '{club_name}'")
    print(f"{runners_html}")
    ## <option value="49026">BAILLY Hyacinthe | 8541421 | H40</option>
    regex = re.compile(r'<option[^>]*\bvalue\s*=\s*(?:"?(\d+)"?)[^>]*>\s*([^|]+?)\s*\|\s*(\d+)\s*\|\s*([^<]+?)\s*</option>',)

    runners: List[Runner] = []
    for m in regex.finditer(runners_html):
        license_id = m.group(1).strip()
        runner_name = m.group(2).strip()
        sportident_id = int(m.group(3).strip())
        category = m.group(4).strip()
        runners.append(
            Runner(
                runnerName=runner_name,
                clubName=club_name,
                sportidentId=sportident_id,
                category=category,
                licenseId=license_id,
            )
        )
    return runners


def build_urls(competition_id: int) -> Tuple[str, str]:
    clubs_url = f"{BASE_URL}/inscription1.php?no={competition_id}"
    runners_url = f"{BASE_URL}/inscription2.php?no={competition_id}"
    return clubs_url, runners_url


def run_extraction(output_json_path: str = "ffco_licenses.json") -> None:
    client = RateLimitedHttpClient(min_interval_seconds=1.0)

    print("[INFO] Step 1: Fetching main page to find latest competitionId")
    main_html = client.get_text(MAIN_URL)
    print(f"[DEBUG] Main page length: {len(main_html)}")
    competition_id = find_highest_competition_id(main_html)
    print(f"[INFO] Latest competitionId: {competition_id}")

    clubs_url, runners_url = build_urls(competition_id)

    print("[INFO] Step 2: Fetching clubs list")
    clubs_html = client.get_text(clubs_url)
    print(f"[DEBUG] Clubs page length: {len(clubs_html)}")
    clubs = parse_clubs(clubs_html)
    print(f"[INFO] Clubs found: {len(clubs)}")

    all_runners: List[Runner] = []
    print("[INFO] Step 3: Fetching runners for each club (1 request/second)")
    for idx, club in enumerate(clubs, start=1):
        print(f"[INFO] Club {idx}/{len(clubs)}: {club.club_id} {club.club_name}")
        runners_html = client.post_form_text(runners_url, {"club": club.club_id})
        print(f"[DEBUG] Runners page length (club {club.club_id}): {len(runners_html)}")
        runners = parse_runners(runners_html, club.club_name)
        print(f"[INFO] Runners parsed for club {club.club_id}: {len(runners)}")
        all_runners.extend(runners)

    print(f"[INFO] Total runners collected: {len(all_runners)}")

    data = {"runners": [r.__dict__ for r in all_runners]}
    with open(output_json_path, "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, indent=2)

    print(f"[INFO] Wrote JSON to: {output_json_path}")


if __name__ == "__main__":
    # NOTE: This script performs real HTTP requests when executed.
    # Review the code before running, as requested.
    year = time.localtime().tm_year
    output_json_path = f"ffco_licenses_${year}.json"
    run_extraction(output_json_path)

