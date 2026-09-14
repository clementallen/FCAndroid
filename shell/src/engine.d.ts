/**
 * The engine's JavaScript surface.
 *
 * Hand-written to match the @JSExport methods on
 * web/src/main/java/com/cloudwalk/web/FlightClub.java. TeaVM does not emit
 * type declarations, so if a method is added there it has to be added here
 * too - and `tsc --noEmit` in the build will catch a mismatch in the shell's
 * use of it, though not a drift in the Java signature itself.
 */
declare module '@engine' {
  export class FlightClub {
    constructor();

    /** Builds a game on the canvas and leaves it in demo mode. */
    start(
      canvasId: string,
      task: string,
      pilotType: number,
      aiCount: number,
      soundBase: string,
    ): void;

    /** The Start button: launches the player's glider. */
    launch(pilotType: number): void;

    /**
     * Runs a frame if one is due. Safe to call every animation frame.
     *
     * Takes no timestamp on purpose: it reads the wall clock itself, because
     * model time is epoch-based and a requestAnimationFrame timestamp is not.
     */
    pump(): void;

    setSize(width: number, height: number): void;

    /** Re-pegs model time after the tab was hidden. */
    reanchor(): void;

    stop(): void;

    setMessageHandler(handler: (text: string) => void): void;
    setDialogHandler(handler: (title: string, text: string) => void): void;

    pointerDown(x: number, y: number): void;
    pointerMove(x: number, y: number): void;
    pointerUp(x: number, y: number): void;

    /** -1 left, 0 straight, 1 right. */
    steer(direction: number): void;
    faster(): void;
    slower(): void;

    setCameraMode(mode: number): void;
    zoomIn(): void;
    zoomOut(): void;
    togglePause(): void;

    /** Freezes or resumes the world. Resuming re-pegs model time. */
    setPaused(paused: boolean): void;
    isPaused(): boolean;

    /** Must be called from a user gesture. */
    resumeAudio(): void;
    setMuted(muted: boolean): void;

    getInfoText(): string;
    getVario(): number;
    getHeading(): number;
    getSpeed(): number;
    getAltitude(): number;
    getDistanceFlown(): number;
    isOnGround(): boolean;
    isFinished(): boolean;
    getFrameRate(): number;

    static cameraUser(): number;
    static cameraGaggle(): number;
    static cameraPlan(): number;
    static cameraTask(): number;
    static cameraPilot(): number;
  }

  export function main(args: string[], callback: (err?: unknown) => void): void;
}
