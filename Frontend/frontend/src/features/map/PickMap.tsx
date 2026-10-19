import 'leaflet/dist/leaflet.css';
import L from 'leaflet';
import { useEffect } from 'react';
import { MapContainer, Marker, Polyline, TileLayer, useMap, useMapEvents } from 'react-leaflet';

export interface LatLng {
  lat: number;
  lng: number;
}
export type MarkerKind = 'pickup' | 'dropoff' | 'point' | 'courier';
export interface MapMarker {
  id: string;
  kind: MarkerKind;
  position: LatLng;
  title: string;
}

const BANDUNG: [number, number] = [-6.9175, 107.6191];

// Ikon dari SVG sendiri (sesuai desain: jemput = bulat biru, antar = kotak emas).
// Sekaligus menghindari masalah klasik ikon default Leaflet yang "hilang" saat di-bundle Vite/webpack.
const svg: Record<MarkerKind, string> = {
  pickup: '<svg width="28" height="28" viewBox="0 0 28 28"><circle cx="14" cy="14" r="10" fill="#2563EB" stroke="#fff" stroke-width="4"/></svg>',
  dropoff: '<svg width="28" height="28" viewBox="0 0 28 28"><rect x="4" y="4" width="20" height="20" rx="5" fill="#F5B841" stroke="#14213D" stroke-width="3"/></svg>',
  point: '<svg width="28" height="28" viewBox="0 0 28 28"><circle cx="14" cy="14" r="10" fill="#14213D" stroke="#F5B841" stroke-width="4"/></svg>',
  courier: '<svg width="32" height="32" viewBox="0 0 32 32"><circle cx="16" cy="16" r="12" fill="#14213D" stroke="#fff" stroke-width="4"/><circle cx="16" cy="16" r="4" fill="#F5B841"/></svg>',
};
const icons = Object.fromEntries(
  (Object.keys(svg) as MarkerKind[]).map((k) => [
    k,
    L.divIcon({ html: svg[k], className: '', iconSize: k === 'courier' ? [32, 32] : [28, 28], iconAnchor: k === 'courier' ? [16, 16] : [14, 14] }),
  ]),
) as Record<MarkerKind, L.DivIcon>;

interface Props {
  markers: MapMarker[];
  onPick?: (pos: LatLng) => void;   // ada → peta bisa diklik untuk memilih titik
  showRoute?: boolean;               // garis putus-putus antar titik (bukan rute jalan sungguhan)
  className?: string;
}

export function PickMap({ markers, onPick, showRoute = false, className = 'h-72' }: Props) {
  return (
    // isolate → z-index internal Leaflet (400+) tidak menimpa header sticky aplikasi
    <div className={`isolate overflow-hidden rounded-2xl ${className}`}>
      <MapContainer center={BANDUNG} zoom={13} scrollWheelZoom className="size-full">
        <TileLayer
          attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
          url="https://tile.openstreetmap.org/{z}/{x}/{y}.png"
        />
        {markers.map((m) => (
          <Marker key={m.id} position={[m.position.lat, m.position.lng]} icon={icons[m.kind]} title={m.title} />
        ))}
        {showRoute && markers.length >= 2 && (
          <Polyline
            positions={markers.map((m) => [m.position.lat, m.position.lng] as [number, number])}
            pathOptions={{ color: '#2563EB', weight: 4, dashArray: '2 10', lineCap: 'round' }}
          />
        )}
        {onPick && <ClickToPick onPick={onPick} />}
        <FitToMarkers markers={markers} />
      </MapContainer>
    </div>
  );
}

function ClickToPick({ onPick }: { onPick: (pos: LatLng) => void }) {
  useMapEvents({ click: (e) => onPick({ lat: e.latlng.lat, lng: e.latlng.lng }) });
  return null;
}

/** Jembatan deklaratif → imperatif: setiap kali posisi marker berubah, atur ulang tampilan peta */
function FitToMarkers({ markers }: { markers: MapMarker[] }) {
  const map = useMap();
  const signature = markers.map((m) => `${m.position.lat},${m.position.lng}`).join('|');
  useEffect(() => {
    if (markers.length === 1) {
      map.setView([markers[0].position.lat, markers[0].position.lng], Math.max(map.getZoom(), 15));
    } else if (markers.length > 1) {
      map.fitBounds(L.latLngBounds(markers.map((m) => [m.position.lat, m.position.lng] as [number, number])), {
        padding: [40, 40],
        maxZoom: 16,
      });
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps -- cukup bereaksi saat POSISI berubah, bukan saat array baru dibuat
  }, [signature, map]);
  return null;
}