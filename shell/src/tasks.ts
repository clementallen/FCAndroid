/**
 * The tasks and gliders on offer.
 *
 * Only t001, t002 and t003 are files in assets/; the rest are generated
 * procedurally by Task.java, which is why their ids look like that. Mirrors
 * ChooseActivity's list so both platforms offer the same thing.
 */

export interface TaskDesc {
  id: string;
  title: string;
  desc: string;
}

export const TASKS: readonly TaskDesc[] = [
  { id: 'default', title: 'Task 1', desc: '50km, 2 turnpoints, cloudbase 1500m' },
  { id: 't001', title: 'Task 2', desc: '100km, cloudbase 1500m' },
  { id: 't002', title: 'Task 3', desc: '70km, cloudbase 1600m' },
  { id: 't003', title: 'Task 4', desc: '120km, 4 turnpoints, cloudbase 1600m' },
  { id: 'default5', title: 'Task 5', desc: '150km, 6 turnpoints, cloudbase 1500m' },
  { id: 'default6', title: 'Task 6', desc: 'Free distance, cloudbase 1500m' },
  { id: 'default7', title: 'Task 7', desc: '160km, 3 turnpoints, cloudbase ~1200m' },
  { id: 'default8', title: 'Task 8', desc: '160km, 1 turnpoint, cloudbase ~1800m' },
  { id: 'default9', title: 'Task 9', desc: '50km, 2 turnpoints, cloudbase ~1500m' },
  { id: 'default10', title: 'Task 10', desc: '80km, 3 turnpoints, cloudbase ~2000m' },
  { id: 'default11', title: 'Task 11', desc: '150km, cloudbase ~3000m' },
];

export interface GliderDesc {
  /** The pilotType the engine expects. */
  type: number;
  name: string;
  desc: string;
}

export const GLIDERS: readonly GliderDesc[] = [
  { type: 0, name: 'Paraglider', desc: 'Slow and forgiving. Turns tightly in weak lift.' },
  { type: 1, name: 'Hang glider', desc: 'Faster, flatter glide, wider turns.' },
  { type: 2, name: 'Sailplane', desc: 'Fastest and flattest. Needs strong lift and space.' },
];
