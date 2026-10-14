import type { FlightClub } from '@engine';

/**
 * The instrument overlay, laid out as android/res/layout/activity_startwifigame.xml
 * lays it out: the status text bottom-left, black with a white shadow and no
 * panel; the variometer and compass side by side at the bottom centre. The
 * dot and compass are Android's own drawables.
 *
 * Polled a few times a second rather than every frame: the underlying model
 * updates at 5Hz anyway (XCModel.tick), so there is nothing to gain from going
 * faster.
 */

const POLL_MS = 100;

/**
 * Android's SeekBar runs 0..100 with 50 + actualSink / 0.37 * 50, so the dot
 * reaches either end at 0.37 units of climb or sink (StartFlightClub).
 */
const VARIO_FULL_SCALE = 0.37;

export class Hud {
  private timer: number | undefined;

  constructor(
    private readonly fc: FlightClub,
    private readonly root: HTMLElement,
  ) {
    root.innerHTML = `
      <div class="hud-info"></div>
      <div class="hud-instruments">
        <div class="hud-vario" aria-label="Variometer">
          <div class="hud-vario-line"></div>
          <div class="hud-vario-tick"></div>
          <img class="hud-vario-dot" src="hud/dot.png" alt="" width="10" height="10" />
        </div>
        <img class="hud-compass" src="hud/notched_compass.png" alt="Compass" width="30" height="30" />
      </div>
      <div class="hud-fps"></div>`;
  }

  private q<T extends Element>(sel: string): T {
    const el = this.root.querySelector<T>(sel);
    if (!el) throw new Error(`missing HUD element ${sel}`);
    return el;
  }

  start(showFps: boolean): void {
    const dot = this.q<HTMLElement>('.hud-vario-dot');
    const compass = this.q<HTMLElement>('.hud-compass');
    const info = this.q<HTMLElement>('.hud-info');
    const fps = this.q<HTMLElement>('.hud-fps');
    fps.hidden = !showFps;

    this.timer = window.setInterval(() => {
      const v = this.fc.getVario() / VARIO_FULL_SCALE;
      dot.style.left = `${50 + Math.max(-1, Math.min(1, v)) * 50}%`;
      compass.style.transform = `rotate(${this.fc.getHeading()}deg)`;
      info.innerHTML = this.fc.getInfoText();
      if (showFps) fps.textContent = `${this.fc.getFrameRate()} fps`;
    }, POLL_MS);
  }

  stop(): void {
    if (this.timer !== undefined) {
      window.clearInterval(this.timer);
      this.timer = undefined;
    }
  }
}
