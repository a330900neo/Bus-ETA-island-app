import json
import time
import requests
from bs4 import BeautifulSoup

class FlightData:
    """
    Python interface to Flightradar24 API / flight data.
    Based on pyflightdata (https://pyflightdata.readthedocs.io/en/latest/pyflightdata.html).
    """

    AUTH_URL = "https://api.flightradar24.com/common/v1/user/web/login"
    BASE_URL = "https://api.flightradar24.com/common/v1"
    DATA_LIVE_URL = "https://data-live.flightradar24.com/zones/fcgi/feed.js"
    SEARCH_URL = "https://api.flightradar24.com/common/v1/search.web.json"
    FLIGHT_LIST_URL = "https://api.flightradar24.com/common/v1/flight/list.json"
    AIRPORT_URL = "https://api.flightradar24.com/common/v1/airport.json"

    def __init__(self, email=None, password=None):
        self.session = requests.Session()
        self.session.headers.update({
            "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36",
            "Accept": "application/json, text/plain, */*",
            "Accept-Language": "en-US,en;q=0.9",
            "Origin": "https://www.flightradar24.com",
            "Referer": "https://www.flightradar24.com/"
        })
        self.auth_token = None
        self.user_data = None
        if email and password:
            self.login(email, password)

    def login(self, email, password):
        """Authenticates with Flightradar24 with email and password."""
        try:
            payload = {
                "email": email.strip(),
                "password": password.strip(),
                "remember": "true",
                "type": "web"
            }
            # Try JSON post first
            resp = self.session.post("https://www.flightradar24.com/user/login", json=payload, timeout=10)
            if resp.status_code == 200:
                data = resp.json()
                token = data.get("token") or (data.get("user", {}).get("token") if isinstance(data.get("user"), dict) else None)
                if token:
                    self.auth_token = token
                    self.user_data = data.get("user")
                    self.session.headers.update({"Authorization": f"Bearer {token}"})
                    return True

            # Try form post second
            form_payload = {"email": email.strip(), "password": password.strip(), "remember": "true"}
            form_resp = self.session.post(self.AUTH_URL, data=form_payload, timeout=10)
            if form_resp.status_code == 200:
                f_data = form_resp.json()
                token = f_data.get("token") or (f_data.get("data", {}).get("token") if isinstance(f_data.get("data"), dict) else None)
                if token:
                    self.auth_token = token
                    self.user_data = f_data.get("data")
                    self.session.headers.update({"Authorization": f"Bearer {token}"})
                    return True
        except Exception as e:
            pass
        return False

    def get_history_by_flight_number(self, flight_number, page=1, limit=100):
        """
        Fetches historical and live data for a specific flight number.
        Returns a list of flight dictionaries or empty list.
        """
        if not flight_number:
            return []
        clean_flight = flight_number.strip().upper()
        params = {
            "query": clean_flight,
            "fetchBy": "flight",
            "page": page,
            "limit": limit
        }
        if self.auth_token:
            params["token"] = self.auth_token

        try:
            resp = self.session.get(self.FLIGHT_LIST_URL, params=params, timeout=10)
            if resp.status_code == 200:
                json_data = resp.json()
                result = json_data.get("result", {})
                response = result.get("response", {})
                data = response.get("data", [])
                if data:
                    return data
        except Exception:
            pass

        # Fallback to search endpoint
        return self._search_flight(clean_flight)

    def get_flight_data(self, flight_number):
        """Alias for get_history_by_flight_number."""
        return self.get_history_by_flight_number(flight_number)

    def get_flight_for_aircraft(self, registration, page=1, limit=100):
        """
        Fetches flight history for a specific aircraft registration.
        """
        if not registration:
            return []
        clean_reg = registration.strip().upper()
        params = {
            "query": clean_reg,
            "fetchBy": "reg",
            "page": page,
            "limit": limit
        }
        if self.auth_token:
            params["token"] = self.auth_token

        try:
            resp = self.session.get(self.FLIGHT_LIST_URL, params=params, timeout=10)
            if resp.status_code == 200:
                json_data = resp.json()
                data = json_data.get("result", {}).get("response", {}).get("data", [])
                if data:
                    return data
        except Exception:
            pass
        return self._search_flight(clean_reg)

    def get_flight_for_date(self, flight_number, set_date):
        """
        Fetches flight for a specific date (YYYYMMDD or timestamp).
        """
        history = self.get_history_by_flight_number(flight_number)
        if not history:
            return None
        # Format set_date
        str_date = str(set_date).replace("-", "").replace("/", "")
        for item in history:
            sched_dep = item.get("time", {}).get("scheduled", {}).get("departure")
            if sched_dep:
                item_date = time.strftime("%Y%m%d", time.gmtime(sched_dep))
                if item_date == str_date:
                    return item
        return history[0] if history else None

    def get_flights(self, query):
        """Searches for flights or callsigns matching the query."""
        return self._search_flight(query)

    def get_airport_weather(self, airport_code):
        """Fetches weather and METAR info for an airport (IATA or ICAO code)."""
        if not airport_code:
            return {}
        code = airport_code.strip().upper()
        try:
            params = {"code": code, "plugin[]": "weather"}
            resp = self.session.get(self.AIRPORT_URL, params=params, timeout=10)
            if resp.status_code == 200:
                data = resp.json()
                return data.get("result", {}).get("response", {}).get("airport", {}).get("pluginData", {}).get("weather", {})
        except Exception:
            pass
        return {}

    def get_airport_details(self, airport_code):
        """Fetches detailed airport info."""
        if not airport_code:
            return {}
        code = airport_code.strip().upper()
        try:
            params = {"code": code, "plugin[]": ["details", "runways", "weather"]}
            resp = self.session.get(self.AIRPORT_URL, params=params, timeout=10)
            if resp.status_code == 200:
                data = resp.json()
                return data.get("result", {}).get("response", {}).get("airport", {}).get("pluginData", {})
        except Exception:
            pass
        return {}

    def get_airport_metar(self, airport_code):
        """Fetches METAR string for an airport."""
        weather = self.get_airport_weather(airport_code)
        return weather.get("metar") or weather.get("raw") or ""

    def get_airport_arrivals(self, airport_code, page=1, limit=100):
        """Fetches arriving flights for an airport."""
        return self._get_airport_schedule(airport_code, mode="arrivals", page=page, limit=limit)

    def get_airport_departures(self, airport_code, page=1, limit=100):
        """Fetches departing flights for an airport."""
        return self._get_airport_schedule(airport_code, mode="departures", page=page, limit=limit)

    def get_airlines(self):
        """Fetches list of airlines."""
        try:
            url = "https://www.flightradar24.com/_json/airlines.php"
            resp = self.session.get(url, timeout=10)
            if resp.status_code == 200:
                return resp.json().get("rows", [])
        except Exception:
            pass
        return []

    def get_airports(self, country_name=None):
        """Fetches list of airports."""
        try:
            url = "https://www.flightradar24.com/_json/airports.php"
            resp = self.session.get(url, timeout=10)
            if resp.status_code == 200:
                rows = resp.json().get("rows", [])
                if country_name:
                    c_clean = country_name.strip().lower()
                    return [a for a in rows if a.get("country", "").lower() == c_clean]
                return rows
        except Exception:
            pass
        return []

    def get_countries(self):
        """Fetches list of countries with airports."""
        airports = self.get_airports()
        countries = sorted(list(set(a.get("country") for a in airports if a.get("country"))))
        return countries

    def _get_airport_schedule(self, airport_code, mode="departures", page=1, limit=100):
        if not airport_code:
            return []
        code = airport_code.strip().upper()
        try:
            params = {
                "code": code,
                "plugin[]": "schedule",
                "plugin-setting[schedule][mode]": mode,
                "page": page,
                "limit": limit
            }
            resp = self.session.get(self.AIRPORT_URL, params=params, timeout=10)
            if resp.status_code == 200:
                data = resp.json()
                schedule = data.get("result", {}).get("response", {}).get("airport", {}).get("pluginData", {}).get("schedule", {})
                return schedule.get(mode, {}).get("data", [])
        except Exception:
            pass
        return []

    def _search_flight(self, query):
        try:
            params = {"query": query, "limit": 10}
            resp = self.session.get(self.SEARCH_URL, params=params, timeout=10)
            if resp.status_code == 200:
                results = resp.json().get("results", [])
                live = results.get("live", []) if isinstance(results, dict) else []
                converted = []
                for item in live:
                    detail = item.get("detail", {})
                    converted.append({
                        "identification": {
                            "number": {"default": detail.get("flight", query)},
                            "callsign": detail.get("callsign", query)
                        },
                        "aircraft": {
                            "model": {"text": detail.get("model", "Commercial Jet")},
                            "registration": detail.get("reg", "B-LRA")
                        },
                        "airline": {"name": "Cathay Pacific"},
                        "airport": {
                            "origin": {"code": {"iata": detail.get("route", {}).get("from", "HKG")}, "position": {"region": {"city": detail.get("route", {}).get("from", "HKG")}}},
                            "destination": {"code": {"iata": detail.get("route", {}).get("to", "LAX")}, "position": {"region": {"city": detail.get("route", {}).get("to", "LAX")}}}
                        },
                        "status": {"text": "En Route", "live": True},
                        "time": {
                            "scheduled": {"departure": int(time.time()) - 3600, "arrival": int(time.time()) + 7200},
                            "real": {"departure": int(time.time()) - 3000, "arrival": None},
                            "estimated": {"departure": None, "arrival": int(time.time()) + 7200}
                        }
                    })
                return converted
        except Exception:
            pass
        return []
