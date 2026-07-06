import { useEffect, useRef } from 'react';
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';
import markerIcon2x from 'leaflet/dist/images/marker-icon-2x.png';
import markerIcon from 'leaflet/dist/images/marker-icon.png';
import markerShadow from 'leaflet/dist/images/marker-shadow.png';
import { t, tp } from '../i18n';
import styles from './PostsMap.module.css';

// Classic Vite fix: Leaflet's runtime icon-path detection fails under bundlers,
// so point the default icon at explicitly imported assets (otherwise 404s).
delete L.Icon.Default.prototype._getIconUrl;
L.Icon.Default.mergeOptions({
  iconRetinaUrl: markerIcon2x,
  iconUrl: markerIcon,
  shadowUrl: markerShadow,
});

// Default view when no location is chosen: rough center of Germany.
const GERMANY_CENTER = [51.1, 10.4];
const GERMANY_ZOOM = 6;
const LOCATION_ZOOM = 11;
const MAX_POPUP_LINKS = 5;

// Popup content is built with DOM nodes (textContent), never innerHTML,
// so user-provided titles cannot inject markup.
function buildPopup(group, basePath) {
  const div = document.createElement('div');
  div.className = styles.popup;
  if (group[0].city || group[0].postalCode) {
    const head = document.createElement('strong');
    head.textContent = [group[0].postalCode, group[0].city].filter(Boolean).join(' ');
    div.appendChild(head);
  }
  const ul = document.createElement('ul');
  ul.className = styles.popupList;
  group.slice(0, MAX_POPUP_LINKS).forEach((item) => {
    const li = document.createElement('li');
    const a = document.createElement('a');
    a.href = `${basePath}/${item.id}`;
    a.textContent = item.title;
    li.appendChild(a);
    ul.appendChild(li);
  });
  div.appendChild(ul);
  if (group.length > MAX_POPUP_LINKS) {
    const more = document.createElement('p');
    more.className = styles.popupMore;
    more.textContent = tp('map.more', { n: group.length - MAX_POPUP_LINKS });
    div.appendChild(more);
  }
  return div;
}

/**
 * Leaflet map of posts (AP 3.2). Privacy: item lat/lon are PLZ centroids
 * resolved server-side from the local plz_geo table — never exact addresses.
 * Items sharing a postal code are grouped into ONE marker with a count badge
 * (simple client-side clustering by PLZ — no plugin needed).
 *
 * Props: items (search results with lat/lon/postalCode), basePath
 * ("/offers" | "/requests"), center (optional { lat, lon }).
 */
export default function PostsMap({ items, basePath, center }) {
  const containerRef = useRef(null);
  const mapRef = useRef(null);
  const layerRef = useRef(null);
  // The initial view is fixed at mount; later center changes are handled
  // by the parent remounting or the user panning.
  const initialCenterRef = useRef(center);

  useEffect(() => {
    const initial = initialCenterRef.current;
    const map = L.map(containerRef.current, { scrollWheelZoom: true }).setView(
      initial ? [initial.lat, initial.lon] : GERMANY_CENTER,
      initial ? LOCATION_ZOOM : GERMANY_ZOOM,
    );
    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors',
      maxZoom: 19,
    }).addTo(map);
    layerRef.current = L.layerGroup().addTo(map);
    mapRef.current = map;
    return () => {
      map.remove();
      mapRef.current = null;
      layerRef.current = null;
    };
  }, []);

  useEffect(() => {
    const layer = layerRef.current;
    if (!layer) return;
    layer.clearLayers();

    // Group by PLZ so one marker represents all posts in that postal code.
    const groups = new Map();
    for (const item of items) {
      if (item.lat == null || item.lon == null) continue;
      const key = item.postalCode ?? `${item.lat},${item.lon}`;
      if (!groups.has(key)) groups.set(key, []);
      groups.get(key).push(item);
    }

    for (const group of groups.values()) {
      const { lat, lon } = group[0];
      const marker = group.length > 1
        ? L.marker([lat, lon], {
            icon: L.divIcon({
              className: styles.clusterIcon,
              html: `<span>${group.length}</span>`,
              iconSize: [34, 34],
            }),
          })
        : L.marker([lat, lon]);
      marker.bindPopup(buildPopup(group, basePath));
      marker.addTo(layer);
    }
  }, [items, basePath]);

  return (
    <div
      ref={containerRef}
      className={styles.map}
      role="region"
      aria-label={t('map.label')}
      data-testid="posts-map"
    />
  );
}
