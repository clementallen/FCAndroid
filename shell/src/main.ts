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

function showDialog(title: string, text: string): void {
  const dlg = el<HTMLDialogElement>('#dialog');
  el('#dialog-title').textContent = title;
  el('#dialog-body').innerHTML = text.replace(/\n/g, '<br>');
  // Nothing should move behind the task briefing. The engine raises this while
  // loading the task, so the gaggle would otherwise be airborne and gone by the
  // time the player has read it.
  fc?.setPaused(true);
  dlg.showModal();
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
  if (el<HTMLDialogElement>('#dialog').open) fc.setPaused(true);

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
  fc?.togglePause();
  el('#pause').textContent = fc?.isPaused() ? 'Resume' : 'Pause';
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
    fc?.setPaused(false);
  });

  window.addEventListener('resize', () => fitCanvas(el<HTMLCanvasElement>('#gl')));

  // Model time runs off the wall clock, but requestAnimationFrame stops while
  // a tab is hidden. Without this the world lurches forward by however long
  // the player was away.
  document.addEventListener('visibilitychange', () => {
    if (!document.hidden) fc?.reanchor();
  });

  document.body.dataset.screen = 'menu';
}

main();
