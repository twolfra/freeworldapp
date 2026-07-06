// Persisted search location — shared by OfferList and RequestList so the
// chosen place survives navigation and revisits.
const LOCATION_KEY = 'fw_location';

export function readStoredLocation() {
  try {
    const loc = JSON.parse(localStorage.getItem(LOCATION_KEY) || 'null');
    return loc && loc.lat != null && loc.lon != null && loc.plz ? loc : null;
  } catch {
    return null;
  }
}

export function saveStoredLocation(loc) {
  localStorage.setItem(LOCATION_KEY, JSON.stringify({
    plz: loc.plz, city: loc.city, lat: loc.lat, lon: loc.lon,
  }));
}

export function clearStoredLocation() {
  localStorage.removeItem(LOCATION_KEY);
}

export function formatLocation(loc) {
  return loc ? `${loc.plz} ${loc.city}` : '';
}
