import "leaflet/dist/leaflet.css";
import L from "leaflet";
import { useEffect } from "react";
import {
  MapContainer,
  Marker,
  Polyline,
  TileLayer,
  useMap,
  useMapEvents,
} from "react-leaflet";

export interface LatLng {
  lat: number;
  lng: number;
}
export type MarkerKind = "pickup" | "dropoff" | "point" | "courier";
export interface MapMarker {
  id: string;
  kind: MarkerKind;
  position: LatLng;
  title: string;
}

const BANDUNG: [number, number] = [-6.9175, 107.6191];

const svg: Record<MarkerKind, string> = {
  pickup:
    '<svg width="28" height="28" viewBox="0 0 28 28"><circle cx="14" cy="14" r="10" fill="#2563EB" stroke="#fff" stroke-width="4"/></svg>',
  dropoff:
    '<svg width="28" height="28" viewBox="0 0 28 28"><rect x="4" y="4" width="20" height="20" rx="5" fill="#F5B841" stroke="#14213D" stroke-width="3"/></svg>',
  point:
    '<svg width="28" height="28" viewBox="0 0 28 28"><circle cx="14" cy="14" r="10" fill="#14213D" stroke="#F5B841" stroke-width="4"/></svg>',
  courier:
    '<svg width="32" height="32" viewBox="0 0 32 32"><circle cx="16" cy="16" r="12" fill="#14213D" stroke="#fff" stroke-width="4"/><circle cx="16" cy="16" r="4" fill="#F5B841"/></svg>',
};
const icons = Object.fromEntries(
  (Object.keys(svg) as MarkerKind[]).map((k) => {
    const size = k === "courier" ? 32 : 28;
    return [
      k,
      L.divIcon({
        html: svg[k],
        className: "",
        iconSize: [size, size],
        iconAnchor: [size / 2, size / 2],
      }),
    ];
  }),
) as Record<MarkerKind, L.DivIcon>;

const toTuple = (p: LatLng) => [p.lat, p.lng] as [number, number];

interface Props {
  markers: MapMarker[];
  onPick?: (pos: LatLng) => void; // ada → peta bisa diklik untuk memilih titik
  showRoute?: boolean; // garis putus-putus jemput → antar (rencana, bukan rute jalan)
  path?: LatLng[]; // garis solid: jejak yang sudah dilalui kurir
  className?: string;
}

export function PickMap({
  markers,
  onPick,
  showRoute = false,
  path = [],
  className = "h-72",
}: Props) {
  const planned = markers.filter(
    (m) => m.kind === "pickup" || m.kind === "dropoff",
  );
  return (
    // isolate → z-index internal Leaflet (400+) tidak menimpa header sticky aplikasi
    <div className={`isolate overflow-hidden rounded-2xl ${className}`}>
      <MapContainer
        center={BANDUNG}
        zoom={13}
        scrollWheelZoom
        className="size-full"
      >
        <TileLayer
          attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
          url="https://tile.openstreetmap.org/{z}/{x}/{y}.png"
        />
        {showRoute && planned.length >= 2 && (
          <Polyline
            positions={planned.map((m) => toTuple(m.position))}
            pathOptions={{
              color: "#2563EB",
              weight: 4,
              dashArray: "2 10",
              lineCap: "round",
              opacity: 0.7,
            }}
          />
        )}
        {path.length >= 2 && (
          <Polyline
            positions={path.map(toTuple)}
            pathOptions={{ color: "#14213D", weight: 5, lineCap: "round" }}
          />
        )}
        {markers.map((m) => (
          <Marker
            key={m.id}
            position={toTuple(m.position)}
            icon={icons[m.kind]}
            title={m.title}
          />
        ))}
        {onPick && <ClickToPick onPick={onPick} />}
        <FitToMarkers markers={markers} />
      </MapContainer>
    </div>
  );
}

function ClickToPick({ onPick }: { onPick: (pos: LatLng) => void }) {
  useMapEvents({
    click: (e) => onPick({ lat: e.latlng.lat, lng: e.latlng.lng }),
  });
  return null;
}

/**
 * Atur ulang tampilan saat titik TETAP (jemput/antar/titik) berubah.
 * Posisi kurir sengaja diabaikan — kalau tidak, peta "melompat" setiap 3 detik saat customer sedang menggeser peta.
 */
function FitToMarkers({ markers }: { markers: MapMarker[] }) {
  const map = useMap();
  const fixed = markers.filter((m) => m.kind !== "courier");
  const target = fixed.length > 0 ? fixed : markers; // hanya ada kurir → ikuti kurir sekali
  const signature = target
    .map((m) => `${m.position.lat},${m.position.lng}`)
    .join("|");

  useEffect(() => {
    if (target.length === 1) {
      map.setView(toTuple(target[0].position), Math.max(map.getZoom(), 15));
    } else if (target.length > 1) {
      map.fitBounds(L.latLngBounds(target.map((m) => toTuple(m.position))), {
        padding: [40, 40],
        maxZoom: 16,
      });
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps -- cukup bereaksi saat POSISI berubah
  }, [signature, map]);
  return null;
}
