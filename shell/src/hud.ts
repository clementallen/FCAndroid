import type { FlightClub } from '@engine';

/**
 * The instrument overlay.
 *
 * On Android this was a TextView, a SeekBar for the variometer and a rotated
 * ImageView for the compass - and nothing else, which is the whole of what
 * this shows too. Here it is DOM over the canvas, polled a few times a second
 * rather than every frame: the underlying model updates at 5Hz anyway
 * (XCModel.tick), so there is nothing to gain from going faster.
 */

const POLL_MS = 100;

export class Hud {
  private timer: number | undefined;

  constructor(
    private readonly fc: FlightClub,
    private readonly root: HTMLElement,
  ) {
    root.innerHTML = `
      <div class="hud-left">
        <div class="hud-vario" aria-label="Variometer">
          <div class="hud-vario-track"><div class="hud-vario-fill"></div></div>
          <div class="hud-vario-label">0.0</div>
        </div>
      </div>
      <div class="hud-centre"><div class="hud-info"></div></div>
      <div class="hud-right">
        <div class="hud-compass" aria-label="Compass"><div class="hud-needle"></div></div>
      </div>
      <div class="hud-fps"></div>`;
  }

  private q<T extends Element>(sel: string): T {
    const el = this.root.querySelector<T>(sel);
    if (!el) throw new Error(`missing HUD element ${sel}`);
    return el;
  }

  start(showFps: boolean): void {
    const fill = this.q<HTMLElement>('.hud-vario-fill');
    const varioLabel = this.q<HTMLElement>('.hud-vario-label');
    const needle = this.q<HTMLElement>('.hud-needle');
    const info = this.q<HTMLElement>('.hud-info');
    const fps = this.q<HTMLElement>('.hud-fps');
    fps.hidden = !showFps;

    this.timer = window.setInterval(() => {
      // Model units: 1 unit of height is ~1000m, 1 unit of distance ~1km.
      const vario = this.fc.getVario();
      const clamped = Math.max(-1, Math.min(1, vario * 4));
      fill.style.height = `${Math.abs(clamped) * 50}%`;
      fill.style.bottom = clamped >= 0 ? '50%' : `${50 - Math.abs(clamped) * 50}%`;
      fill.classList.toggle('sinking', clamped < 0);
      varioLabel.textContent = vario.toFixed(1);

      needle.style.transform = `rotate(${this.fc.getHeading()}deg)`;
      // The engine's own status line already reports height, speed and
      // distance flown (GliderTask.getStatusMsg), which is all Android shows.
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
