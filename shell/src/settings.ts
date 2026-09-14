/**
 * Player settings.
 *
 * The engine reads these through WebPrefs, which is localStorage under an
 * "fc." prefix. Keys and defaults match android/res/xml/preferences.xml so a
 * setting means the same thing on both platforms. The engine never writes
 * them; this is the only writer.
 */

const PREFIX = 'fc.';

export interface Setting {
  key: string;
  label: string;
  kind: 'int' | 'bool' | 'color' | 'choice';
  default: string;
  choices?: readonly string[];
  group: string;
  hint?: string;
}

export const SETTINGS: readonly Setting[] = [
  { group: 'Gaggle', key: 'numpg', label: 'Paragliders', kind: 'int', default: '3' },
  { group: 'Gaggle', key: 'numhg', label: 'Hang gliders', kind: 'int', default: '3' },
  { group: 'Gaggle', key: 'numsp', label: 'Sailplanes', kind: 'int', default: '3' },
  { group: 'Gaggle', key: 'birds', label: 'Birds', kind: 'int', default: '2' },

  { group: 'Graphics', key: 'view_angle', label: 'View angle', kind: 'int', default: '10' },
  {
    group: 'Graphics',
    key: 'max_fps',
    label: 'Frame rate cap',
    kind: 'choice',
    default: '25',
    choices: ['15', '20', '25', '30'],
    hint: 'The simulation steps once per frame, so this also sets how fast time passes.',
  },
  { group: 'Graphics', key: 'real_shadows', label: 'Sun-angle shadows', kind: 'bool', default: 'true' },
  { group: 'Graphics', key: 'fps', label: 'Show frame rate', kind: 'bool', default: 'false' },

  { group: 'Colours', key: 'sky_color', label: 'Sky', kind: 'color', default: '#c0fbff' },
  { group: 'Colours', key: 'ground_color', label: 'Ground', kind: 'color', default: '#ffffa0' },
  { group: 'Colours', key: 'tp_color', label: 'Turnpoints', kind: 'color', default: '#aaaa00' },
  { group: 'Colours', key: 'glider_color', label: 'Your glider', kind: 'color', default: '#0000ff' },
  { group: 'Colours', key: 'pilot_color', label: 'Your pilot', kind: 'color', default: '#ffff00' },

  { group: 'Sound', key: 'sink_tone', label: 'Sink tone', kind: 'bool', default: 'true' },
  { group: 'Sound', key: 'ambient_sound', label: 'Wind and birds', kind: 'bool', default: 'true' },

  {
    group: 'Display',
    key: 'show_controls',
    label: 'Show controls screen',
    kind: 'bool',
    default: 'true',
    hint: 'The diagram of which part of the screen does what, shown when a flight starts.',
  },
];

function read(key: string): string | null {
  try {
    return localStorage.getItem(PREFIX + key);
  } catch {
    return null;
  }
}

export function get(setting: Setting): string {
  return read(setting.key) ?? setting.default;
}

export function getInt(key: string, fallback: number): number {
  const v = read(key);
  if (v === null) return fallback;
  const n = Number.parseInt(v, 10);
  return Number.isNaN(n) ? fallback : n;
}

export function getBool(key: string, fallback: boolean): boolean {
  const v = read(key);
  if (v === null) return fallback;
  return v === 'true' || v === '1';
}

export function set(key: string, value: string): void {
  try {
    localStorage.setItem(PREFIX + key, value);
  } catch {
    // private window, or site data blocked - the game still plays, the
    // setting just will not survive a reload
  }
}

/**
 * Colours reach the engine as the signed 32-bit ARGB ints Android's Color
 * class produces, because that is what the engine's Color class expects.
 */
export function setColor(key: string, hex: string): void {
  const rgb = Number.parseInt(hex.replace('#', ''), 16);
  set(key, String((0xff000000 | rgb) | 0));
}

export function getColorHex(setting: Setting): string {
  const v = read(setting.key);
  if (v === null) return setting.default;
  const n = Number.parseInt(v, 10);
  if (Number.isNaN(n)) return setting.default;
  return '#' + (n & 0xffffff).toString(16).padStart(6, '0');
}

/**
 * Writes any setting the player has not chosen, using its default.
 *
 * Android does this through PreferenceManager.setDefaultValues at launch. The
 * engine reads settings directly and falls back to its own hard-coded values
 * when a key is absent - which for the colours means plain white, and a very
 * washed-out looking sky. So seed them the same way Android does.
 */
export function applyDefaults(): void {
  for (const s of SETTINGS) {
    if (read(s.key) !== null) continue;
    if (s.kind === 'color') {
      setColor(s.key, s.default);
    } else {
      set(s.key, s.default);
    }
  }
}

export function reset(): void {
  for (const s of SETTINGS) {
    try {
      localStorage.removeItem(PREFIX + s.key);
    } catch {
      // nothing to do
    }
  }
}
