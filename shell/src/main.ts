import { FlightClub } from '@engine';
import { TASKS, GLIDERS } from './tasks';
import * as settings from './settings';
import { Hud } from './hud';
import { install as installControls } from './controls';
import './style.css';

/**
 * The shell: menus, HUD, settings, and the frame callback.
 *
 * Everything below the canvas is the Java engine compiled by TeaVM; this file
 * owns everything around it. The boundary is the FlightClub class and nothing
 * else.
 */

const el = <T extends HTMLElement>(sel: string): T => {
  const found = document.querySelector<T>(sel);
  if (!found) throw new Error(`missing element ${sel}`);
  return found;
};

const CAMERAS = [
  { label: 'Pilot view', mode: FlightClub.cameraUser() },
  { label: 'Gaggle', mode: FlightClub.cameraGaggle() },
  { label: 'Plan', mode: FlightClub.cameraPlan() },
  { label: 'Task', mode: FlightClub.cameraTask() },
  { label: 'Chase', mode: FlightClub.cameraPilot() },
];

let fc: FlightClub | undefined;
let hud: Hud | undefined;
let teardownControls: (() => void) | undefined;
let rafHandle = 0;

function flash(text: string): void {
  const box = el('#messages');
  const line = document.createElement('div');
  line.className = 'message';
  line.textContent = text;
  box.append(line);
  window.setTimeout(() => line.remove(), 6000);
}

/**
 * Three separate things want the world held still - the task briefing, the
 * pause button, and a phone turned upright - and they overlap. Tracking them
 * as one set and recomputing means dismissing the briefing cannot resume a
 * game the player paused, or one that is only showing a rotate prompt.
 */
type PauseReason = 'dialog' | 'user' | 'portrait' | 'controls';
const pauses = new Set<PauseReason>();

function applyPause(): void {
  fc?.setPaused(pauses.size > 0);
  // Only the button's own reason changes its label - the world being held for
  // a briefing or a sideways phone is not something it should claim to undo.
  el('#pause').textContent = pauses.has('user') ? 'Resume' : 'Pause';
}

function setPause(reason: PauseReason, on: boolean): void {
  if (on) pauses.add(reason);
  else pauses.delete(reason);
  applyPause();
}

function showDialog(title: string, text: string): void {
  const dlg = el<HTMLDialogElement>('#dialog');
  el('#dialog-title').textContent = title;
  el('#dialog-body').innerHTML = text.replace(/\n/g, '<br>');
  // Nothing should move behind the task briefing. The engine raises this while
  // loading the task, so the gaggle would otherwise be airborne and gone by the
  // time the player has read it.
  setPause('dialog', true);
  dlg.showModal();
}

/**
 * A phone held upright - not merely a narrow window.
 *
 * ModelViewRenderer fixes the vertical field of view and lets the horizontal
 * one follow the aspect ratio, so portrait shows under a quarter of the width
 * landscape does. A narrow desktop window has the same aspect but a mouse and
 * keyboard, and rotating a monitor is not an option, so gate on the pointer.
 */
function isPortraitPhone(): boolean {
  return window.matchMedia('(orientation: portrait) and (pointer: coarse)').matches;
}

async function toggleFullscreen(): Promise<void> {
  try {
    if (document.fullscreenElement) {
      await document.exitFullscreen();
      return;
    }
    await document.documentElement.requestFullscreen();
    // Only allowed while fullscreen, and only on platforms that have it -
    // Safari has neither, which is what the manifest is for.
    await (screen.orientation as ScreenOrientation & {
      lock?(o: string): Promise<void>;
    }).lock?.('landscape');
  } catch {
    // Refused (Safari, or a gesture the browser did not like). The button
    // simply does nothing; nothing else depends on it.
  }
}

function fitCanvas(canvas: HTMLCanvasElement): void {
  // Cap at 1x on very dense displays: this is a fill-rate-bound renderer from
  // 2014 and a 3x framebuffer costs far more than it shows.
  const dpr = Math.min(window.devicePixelRatio || 1, 2);
  const w = Math.round(canvas.clientWidth * dpr);
  const h = Math.round(canvas.clientHeight * dpr);
  if (w > 0 && h > 0) fc?.setSize(w, h);
}

function startGame(taskId: string, pilotType: number): void {
  const canvas = el<HTMLCanvasElement>('#gl');

  stopGame();
  fc = new FlightClub();
  fc.setMessageHandler(flash);
  fc.setDialogHandler(showDialog);

  const ai =
    settings.getInt('numpg', 3) +
    settings.getInt('numhg', 3) +
    settings.getInt('numsp', 3);

  // Show the game first: while the menu is up, #game is display:none and the
  // canvas has no layout size, so sizing it here would start the engine with
  // a 0x0 viewport.
  document.body.dataset.screen = 'playing';
  const dpr = Math.min(window.devicePixelRatio || 1, 2);
  canvas.width = Math.round(canvas.clientWidth * dpr);
  canvas.height = Math.round(canvas.clientHeight * dpr);

  try {
    fc.start('gl', taskId, pilotType, Math.max(1, Math.round(ai / 3)), 'sounds/');
  } catch (e) {
    showDialog('Could not start', String(e));
    fc = undefined;
    document.body.dataset.screen = 'menu';
    return;
  }

  // Audio needs a gesture, and clicking the task was one.
  fc.resumeAudio();
  fc.launch(pilotType);

  // The engine raises the task briefing while loading the task, but
  // XCModel.startPlay() clears any pause right afterwards - so the pause has
  // to be re-applied here, once launching is done, or the gaggle flies away
  // behind the briefing.
  pauses.delete('user');
  if (!el<HTMLDialogElement>('#dialog').open) pauses.delete('dialog');
  if (settings.getBool('show_controls', true)) pauses.add('controls');
  if (isPortraitPhone()) pauses.add('portrait');
  el('#controls-help').hidden = !pauses.has('controls');
  applyPause();

  buildCameraBar();

  hud = new Hud(fc, el('#hud'));
  hud.start(settings.getBool('fps', false));

  teardownControls = installControls(fc, canvas, {
    onPause: () => togglePause(),
    onCamera: (i) => fc?.setCameraMode(CAMERAS[i]?.mode ?? CAMERAS[0]!.mode),
    onZoom: (d) => (d > 0 ? fc?.zoomIn() : fc?.zoomOut()),
  });

  const loop = () => {
    fc?.pump();
    rafHandle = requestAnimationFrame(loop);
  };
  rafHandle = requestAnimationFrame(loop);
}

function stopGame(): void {
  pauses.clear();
  el('#controls-help').hidden = true;
  cancelAnimationFrame(rafHandle);
  rafHandle = 0;
  teardownControls?.();
  teardownControls = undefined;
  hud?.stop();
  hud = undefined;
  fc?.stop();
  fc = undefined;
}

function togglePause(): void {
  setPause('user', !pauses.has('user'));
}

function buildCameraBar(): void {
  const bar = el('#cameras');
  bar.replaceChildren();
  CAMERAS.forEach((cam, i) => {
    const b = document.createElement('button');
    b.textContent = cam.label;
    b.title = `View ${i + 1}`;
    b.addEventListener('click', () => fc?.setCameraMode(cam.mode));
    bar.append(b);
  });
}

function buildMenu(): void {
  // Mirrors the Android chooser: gliders as radios down the left, tasks as a
  // two-column grid, and tapping a task starts it straight away - there is no
  // separate Fly button there.
  let chosenGlider = GLIDERS[0]!.type;

  const gliderList = el('#gliders');
  GLIDERS.forEach((g, i) => {
    const b = document.createElement('button');
    b.className = 'glider';
    b.type = 'button';
    b.setAttribute('role', 'radio');
    b.setAttribute('aria-checked', String(i === 0));
    b.title = g.desc;
    b.innerHTML = `<span class="dot"></span>${g.name}`;
    b.addEventListener('click', () => {
      chosenGlider = g.type;
      for (const other of gliderList.children) other.setAttribute('aria-checked', 'false');
      b.setAttribute('aria-checked', 'true');
    });
    gliderList.append(b);
  });

  const taskList = el('#tasks');
  TASKS.forEach((t) => {
    const b = document.createElement('button');
    b.className = 'task';
    b.type = 'button';
    b.innerHTML = `<strong>${t.title}</strong><span>${t.desc}</span>`;
    b.addEventListener('click', () => startGame(t.id, chosenGlider));
    taskList.append(b);
  });
}

function buildSettings(): void {
  const form = el('#settings-form');
  const groups = new Map<string, HTMLElement>();

  for (const s of settings.SETTINGS) {
    let group = groups.get(s.group);
    if (!group) {
      const fs = document.createElement('fieldset');
      fs.innerHTML = `<legend>${s.group}</legend>`;
      form.append(fs);
      groups.set(s.group, fs);
      group = fs;
    }

    const row = document.createElement('label');
    row.className = 'setting';
    const name = document.createElement('span');
    name.textContent = s.label;
    if (s.hint) name.title = s.hint;
    row.append(name);

    let input: HTMLInputElement | HTMLSelectElement;
    if (s.kind === 'bool') {
      const i = document.createElement('input');
      i.type = 'checkbox';
      i.checked = settings.getBool(s.key, s.default === 'true');
      i.addEventListener('change', () => settings.set(s.key, String(i.checked)));
      input = i;
    } else if (s.kind === 'color') {
      const i = document.createElement('input');
      i.type = 'color';
      i.value = settings.getColorHex(s);
      i.addEventListener('change', () => settings.setColor(s.key, i.value));
      input = i;
    } else if (s.kind === 'choice') {
      const sel = document.createElement('select');
      for (const c of s.choices ?? []) {
        const o = document.createElement('option');
        o.value = o.textContent = c;
        sel.append(o);
      }
      sel.value = settings.get(s);
      sel.addEventListener('change', () => settings.set(s.key, sel.value));
      input = sel;
    } else {
      const i = document.createElement('input');
      i.type = 'number';
      i.min = '0';
      i.max = '20';
      i.value = settings.get(s);
      i.addEventListener('change', () => settings.set(s.key, i.value));
      input = i;
    }
    row.append(input);
    group.append(row);
  }

  el('#settings-reset').addEventListener('click', () => {
    settings.reset();
    settings.applyDefaults();
    form.replaceChildren();
    buildSettings();
  });
}

function main(): void {
  settings.applyDefaults();
  buildMenu();
  buildSettings();

  el('#pause').addEventListener('click', togglePause);
  el('#quit').addEventListener('click', () => {
    stopGame();
    document.body.dataset.screen = 'menu';
  });
  el('#open-settings').addEventListener('click', () =>
    el<HTMLDialogElement>('#settings').showModal(),
  );
  el('#close-settings').addEventListener('click', () =>
    el<HTMLDialogElement>('#settings').close(),
  );
  el('#dialog-ok').addEventListener('click', () => {
    el<HTMLDialogElement>('#dialog').close();
    setPause('dialog', false);
  });

  el('#fullscreen').addEventListener('click', () => void toggleFullscreen());

  // Any tap dismisses the controls screen, as on Android.
  el('#controls-help').addEventListener('click', () => {
    el('#controls-help').hidden = true;
    setPause('controls', false);
  });

  // Tap the status block to fold it down to one line - it is the only thing
  // the HUD puts over the middle of the view.
  el('#hud').addEventListener('click', (e) => {
    const info = (e.target as HTMLElement).closest('.hud-info');
    info?.classList.toggle('collapsed');
  });

  window.addEventListener('resize', () => fitCanvas(el<HTMLCanvasElement>('#gl')));

  // Hold the world while the phone is upright: the rotate prompt covers the
  // canvas, so the flight would otherwise carry on unseen.
  const portrait = window.matchMedia('(orientation: portrait) and (pointer: coarse)');
  portrait.addEventListener('change', (e) => {
    if (!fc) return;
    setPause('portrait', e.matches);
    if (!e.matches) fitCanvas(el<HTMLCanvasElement>('#gl'));
  });

  // Model time runs off the wall clock, but requestAnimationFrame stops while
  // a tab is hidden. Without this the world lurches forward by however long
  // the player was away.
  document.addEventListener('visibilitychange', () => {
    if (!document.hidden) fc?.reanchor();
  });

  document.body.dataset.screen = 'menu';
}

main();
