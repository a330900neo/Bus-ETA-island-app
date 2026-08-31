import json
import traceback

try:
    from pyflightdata import FlightData
except ImportError:
    FlightData = None

_client = None

def init_client(email=None, password=None):
    """Initializes or authenticates pyflightdata FlightData instance."""
    global _client
    if FlightData is None:
        return json.dumps({"success": False, "error": "pyflightdata not installed"})
    try:
        if email and password and email.strip() and password.strip():
            _client = FlightData(email=email.strip(), password=password.strip())
            # Attempt login
            try:
                res = _client.login(email.strip(), password.strip())
                return json.dumps({"success": True, "status": "Logged in", "result": str(res)})
            except Exception as login_err:
                return json.dumps({"success": True, "status": "Created with credentials", "warning": str(login_err)})
        else:
            _client = FlightData()
            return json.dumps({"success": True, "status": "Guest mode initialized"})
    except Exception as e:
        _client = FlightData() if FlightData else None
        return json.dumps({"success": False, "error": str(e), "trace": traceback.format_exc()})

def get_client():
    global _client
    if _client is None and FlightData is not None:
        try:
            _client = FlightData()
        except Exception:
            pass
    return _client

def get_history_by_flight_number(flight_number, page=1, limit=20):
    """Fetches flight history for a flight number using pyflightdata."""
    client = get_client()
    if client is None:
        return json.dumps({"success": False, "error": "FlightData client not available"})
    try:
        data = client.get_history_by_flight_number(flight_number, page=int(page), limit=int(limit))
        return json.dumps({"success": True, "data": data})
    except Exception as e:
        return json.dumps({"success": False, "error": str(e), "trace": traceback.format_exc()})

def get_flight_for_date(flight_number, date_str):
    """Fetches flight data for specific date YYYYMMDD using pyflightdata."""
    client = get_client()
    if client is None:
        return json.dumps({"success": False, "error": "FlightData client not available"})
    try:
        data = client.get_flight_for_date(flight_number, date_str)
        return json.dumps({"success": True, "data": data})
    except Exception as e:
        return json.dumps({"success": False, "error": str(e)})

def get_flight_for_aircraft(registration, page=1, limit=20):
    """Fetches flight history for an aircraft registration using pyflightdata."""
    client = get_client()
    if client is None:
        return json.dumps({"success": False, "error": "FlightData client not available"})
    try:
        data = client.get_flight_for_aircraft(registration, page=int(page), limit=int(limit))
        return json.dumps({"success": True, "data": data})
    except Exception as e:
        return json.dumps({"success": False, "error": str(e)})

def get_flights_by_query(query):
    """Searches flights matching query using pyflightdata."""
    client = get_client()
    if client is None:
        return json.dumps({"success": False, "error": "FlightData client not available"})
    try:
        data = client.get_flights(query)
        return json.dumps({"success": True, "data": data})
    except Exception as e:
        return json.dumps({"success": False, "error": str(e)})

def get_airport_weather(airport_code):
    """Fetches airport weather using pyflightdata."""
    client = get_client()
    if client is None:
        return json.dumps({"success": False, "error": "FlightData client not available"})
    try:
        data = client.get_airport_weather(airport_code)
        return json.dumps({"success": True, "data": data})
    except Exception as e:
        return json.dumps({"success": False, "error": str(e)})

def get_airport_details(airport_code):
    """Fetches airport details using pyflightdata."""
    client = get_client()
    if client is None:
        return json.dumps({"success": False, "error": "FlightData client not available"})
    try:
        data = client.get_airport_details(airport_code)
        return json.dumps({"success": True, "data": data})
    except Exception as e:
        return json.dumps({"success": False, "error": str(e)})

def get_airport_arrivals(airport_code, page=1, limit=20):
    """Fetches airport arrivals using pyflightdata."""
    client = get_client()
    if client is None:
        return json.dumps({"success": False, "error": "FlightData client not available"})
    try:
        data = client.get_airport_arrivals(airport_code, page=int(page), limit=int(limit))
        return json.dumps({"success": True, "data": data})
    except Exception as e:
        return json.dumps({"success": False, "error": str(e)})

def get_airport_departures(airport_code, page=1, limit=20):
    """Fetches airport departures using pyflightdata."""
    client = get_client()
    if client is None:
        return json.dumps({"success": False, "error": "FlightData client not available"})
    try:
        data = client.get_airport_departures(airport_code, page=int(page), limit=int(limit))
        return json.dumps({"success": True, "data": data})
    except Exception as e:
        return json.dumps({"success": False, "error": str(e)})
