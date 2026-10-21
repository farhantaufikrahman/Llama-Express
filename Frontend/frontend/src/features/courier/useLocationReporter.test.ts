import { act, cleanup, renderHook, waitFor } from "@testing-library/react";
import { useLocationReporter } from "./useLocationReporter";
import { courierApi } from "./api";

jest.mock("./api", () => ({
  courierApi: { sendLocation: jest.fn().mockResolvedValue(undefined) },
}));
const sendLocation = jest.mocked(courierApi.sendLocation);

// --- GPS tiruan: menyimpan callback agar test bisa "mengirim" posisi kapan saja ---
let onSuccess: PositionCallback | undefined;
let onError: PositionErrorCallback | null | undefined;
const geo = {
  watchPosition: jest.fn(
    (success: PositionCallback, error?: PositionErrorCallback | null) => {
      onSuccess = success;
      onError = error;
      return 42;
    },
  ),
  clearWatch: jest.fn(),
  getCurrentPosition: jest.fn(),
};

const fix = (lat: number, lng: number) =>
  ({
    coords: {
      latitude: lat,
      longitude: lng,
      speed: null,
      heading: null,
      accuracy: 5,
      altitude: null,
      altitudeAccuracy: null,
    },
    timestamp: Date.now(),
  }) as unknown as GeolocationPosition;

const gpsError = (code: number) =>
  ({
    code,
    message: "",
    PERMISSION_DENIED: 1,
    POSITION_UNAVAILABLE: 2,
    TIMEOUT: 3,
  }) as GeolocationPositionError;

function installGps() {
  Object.defineProperty(navigator, "geolocation", {
    value: geo,
    configurable: true,
  });
}

afterEach(() => {
  cleanup(); // unmount dulu (effect cleanup memanggil clearWatch) sebelum GPS tiruan dicabut
  Reflect.deleteProperty(navigator, "geolocation");
  onSuccess = undefined;
  onError = undefined;
  jest.useRealTimers();
});

describe("useLocationReporter", () => {
  it('offline → status "off" dan GPS tidak dinyalakan', () => {
    installGps();
    const { result } = renderHook(() => useLocationReporter(false));
    expect(result.current.status).toBe("off");
    expect(geo.watchPosition).not.toHaveBeenCalled();
  });

  it('browser tanpa GPS → "unavailable"', () => {
    // jsdom tidak punya navigator.geolocation secara bawaan
    const { result } = renderHook(() => useLocationReporter(true));
    expect(result.current.status).toBe("unavailable");
  });

  it('online → "locating", lalu posisi pertama → "sharing" + langsung dikirim', async () => {
    installGps();
    const { result } = renderHook(() => useLocationReporter(true));
    expect(result.current.status).toBe("locating");

    act(() => onSuccess!(fix(-6.2, 106.8)));

    expect(result.current.status).toBe("sharing");
    expect(result.current.position).toEqual(
      expect.objectContaining({ lat: -6.2, lng: 106.8 }),
    );
    expect(sendLocation).toHaveBeenCalledTimes(1);
    expect(sendLocation).toHaveBeenCalledWith({
      lat: -6.2,
      lng: 106.8,
      speed: undefined,
      heading: undefined,
    });
    await waitFor(() => expect(result.current.lastSentAt).not.toBeNull());
  });

  it("throttle: posisi kedua < 3 detik TIDAK dikirim; heartbeat 10 detik tetap mengirim", async () => {
    jest.useFakeTimers();
    installGps();
    renderHook(() => useLocationReporter(true));

    act(() => onSuccess!(fix(-6.2, 106.8)));
    act(() => onSuccess!(fix(-6.21, 106.81))); // < 3 detik → dibuang
    expect(sendLocation).toHaveBeenCalledTimes(1);

    await act(async () => {
      jest.advanceTimersByTime(10_000); // heartbeat (force) walau kurir diam
    });
    expect(sendLocation).toHaveBeenCalledTimes(2);
  });

  it('izin lokasi ditolak → "denied"; error lain → "unavailable"', () => {
    installGps();
    const { result } = renderHook(() => useLocationReporter(true));

    act(() => onError!(gpsError(1)));
    expect(result.current.status).toBe("denied");

    act(() => onError!(gpsError(2)));
    expect(result.current.status).toBe("unavailable");
  });

  it('gagal kirim (mis. sinyal hilang) → tidak crash, status tetap "sharing"', async () => {
    sendLocation.mockRejectedValueOnce(new Error("network"));
    installGps();
    const { result } = renderHook(() => useLocationReporter(true));

    await act(async () => onSuccess!(fix(-6.2, 106.8)));

    expect(result.current.status).toBe("sharing");
    expect(result.current.lastSentAt).toBeNull();
  });

  it('offline lagi → GPS dimatikan; online lagi → mulai dari "locating"', () => {
    installGps();
    const { result, rerender } = renderHook(
      ({ enabled }) => useLocationReporter(enabled),
      {
        initialProps: { enabled: true },
      },
    );
    act(() => onSuccess!(fix(-6.2, 106.8)));
    expect(result.current.status).toBe("sharing");

    rerender({ enabled: false });
    expect(result.current.status).toBe("off");
    expect(geo.clearWatch).toHaveBeenCalledWith(42);

    rerender({ enabled: true });
    expect(result.current.status).toBe("locating");
  });
});
