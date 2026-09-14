import type { FlightClub } from '@engine';

/**
 * Keyboard and pointer, mapped onto the engine.
 *
 * The Android build steers by which third of the screen you touch, which is
 * right for a phone and wrong for a desktop. Here the keyboard steers and the
 * mouse orbits the camera, with the touch path kept for tablets and phones.
 */

export interface ControlsHost {
  onPause(): void;
  onCamera(mode: number): void;
  onZoom(delta: number): void;
}

const HELD = new Set<string>();

export function install(
  fc: FlightClub,
  canvas: HTMLCanvasElement,
  host: ControlsHost,
): () => void {
  let dragging = false;

  const steerFromKeys = () => {
    const left = HELD.has('ArrowLeft') || HELD.has('KeyA');
    const right = HELD.has('ArrowRight') || HELD.has('KeyD');
    fc.steer(left === right ? 0 : left ? -1 : 1);
  };

  const onKeyDown = (e: KeyboardEvent) => {
    if (e.repeat) return;
    const code = e.code;

    switch (code) {
      case 'ArrowLeft':
      case 'KeyA':
      case 'ArrowRight':
      case 'KeyD':
        HELD.add(code);
        steerFromKeys();
        e.preventDefault();
        return;
      case 'ArrowUp':
      case 'KeyW':
        fc.faster();
        e.preventDefault();
        return;
      case 'ArrowDown':
      case 'KeyS':
        fc.slower();
        e.preventDefault();
        return;
      case 'Space':
        host.onPause();
        e.preventDefault();
        return;
      case 'Equal':
      case 'NumpadAdd':
        host.onZoom(1);
        return;
      case 'Minus':
      case 'NumpadSubtract':
        host.onZoom(-1);
        return;
      default:
        break;
    }

    // 1-5 cut between camera views, in the order the Android buttons use
    const views = ['Digit1', 'Digit2', 'Digit3', 'Digit4', 'Digit5'];
    const idx = views.indexOf(code);
    if (idx >= 0) host.onCamera(idx);
  };

  const onKeyUp = (e: KeyboardEvent) => {
    if (HELD.delete(e.code)) steerFromKeys();
  };

  // A held key with the window unfocused never sends its keyup, which would
  // leave the glider turning forever.
  const onBlur = () => {
    HELD.clear();
    fc.steer(0);
  };

  const pos = (e: PointerEvent): [number, number] => {
    const r = canvas.getBoundingClientRect();
    return [
      ((e.clientX - r.left) / r.width) * canvas.width,
      ((e.clientY - r.top) / r.height) * canvas.height,
    ];
  };

  const isTouch = (e: PointerEvent) => e.pointerType !== 'mouse';

  const onPointerDown = (e: PointerEvent) => {
    canvas.setPointerCapture(e.pointerId);
    dragging = true;
    const [x, y] = pos(e);
    fc.pointerDown(x, y, isTouch(e));
  };

  const onPointerMove = (e: PointerEvent) => {
    if (!dragging) return;
    const [x, y] = pos(e);
    fc.pointerMove(x, y, isTouch(e));
  };

  const onPointerUp = (e: PointerEvent) => {
    if (!dragging) return;
    dragging = false;
    const [x, y] = pos(e);
    fc.pointerUp(x, y, isTouch(e));
  };

  const onWheel = (e: WheelEvent) => {
    host.onZoom(e.deltaY < 0 ? 1 : -1);
    e.preventDefault();
  };

  window.addEventListener('keydown', onKeyDown);
  window.addEventListener('keyup', onKeyUp);
  window.addEventListener('blur', onBlur);
  canvas.addEventListener('pointerdown', onPointerDown);
  canvas.addEventListener('pointermove', onPointerMove);
  canvas.addEventListener('pointerup', onPointerUp);
  canvas.addEventListener('pointercancel', onPointerUp);
  canvas.addEventListener('wheel', onWheel, { passive: false });

  return () => {
    window.removeEventListener('keydown', onKeyDown);
    window.removeEventListener('keyup', onKeyUp);
    window.removeEventListener('blur', onBlur);
    canvas.removeEventListener('pointerdown', onPointerDown);
    canvas.removeEventListener('pointermove', onPointerMove);
    canvas.removeEventListener('pointerup', onPointerUp);
    canvas.removeEventListener('pointercancel', onPointerUp);
    canvas.removeEventListener('wheel', onWheel);
    HELD.clear();
  };
}
