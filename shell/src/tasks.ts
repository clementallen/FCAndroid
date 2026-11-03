/**
 * The tasks and gliders on offer.
 *
 * Only t001, t002 and t003 are files in assets/; the rest are generated
 * procedurally by Task.java, which is why their ids look like that. Titles and
 * descriptions are ChooseActivity's verbatim, so the two chooser screens read
 * identically; the longer glider names live in the tooltips.
 */

export interface TaskDesc {
  id: string;
  title: string;
  desc: string;
}

export const TASKS: readonly TaskDesc[] = [
  { id: 'default', title: 'Task 1', desc: 'D: 50km, TP: 2, CB: 1500m' },
  { id: 't001', title: 'Task 2', desc: 'D: 100km, CB: 1500m' },
  { id: 't002', title: 'Task 3', desc: 'D: 70km, CB: 1600m' },
  { id: 't003', title: 'Task 4', desc: 'D: 120km, TP: 4, CB: 1600m' },
  { id: 'default5', title: 'Task 5', desc: '150km, TP: 6, CB: 1500m' },
  { id: 'default6', title: 'Task 6', desc: 'Free dist., CB: 1500m' },
  { id: 'default7', title: 'Task 7', desc: 'D: 160km, TP: 3, CB: 1200m+-' },
  { id: 'default8', title: 'Task 8', desc: 'D: 160km, TP: 1, CB: 1800m+-' },
  { id: 'default9', title: 'Task 9', desc: 'D: 50km, TP: 2, CB: 1500m+-' },
  { id: 'default10', title: 'Task 10', desc: 'D: 80km, TP: 3, CB: 2000m+-' },
  { id: 'default11', title: 'Task 11', desc: 'D: 150km, CB: 3000m+-' },
];

export interface GliderDesc {
  /** The pilotType the engine expects. */
  type: number;
  name: string;
  desc: string;
}

export const GLIDERS: readonly GliderDesc[] = [
  { type: 0, name: 'PG', desc: 'Paraglider - slow and forgiving. Turns tightly in weak lift.' },
  { type: 1, name: 'HG', desc: 'Hang glider - faster, flatter glide, wider turns.' },
  { type: 2, name: 'Sailplane', desc: 'Fastest and flattest. Needs strong lift and space.' },
];
