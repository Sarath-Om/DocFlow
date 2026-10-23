import Dexie, { type Table } from 'dexie';
import type { Vault } from './crypto';

export type DocumentRecord = { id: string; name: string; type: string; size: number; modified: number; cipher: Blob; wrappedKey: string; iv: string; cloudPath?: string; favorite: boolean; folder: string; deletedAt?: number; availableOffline: boolean };
export type Settings = { id: 'settings'; theme: 'light' | 'dark' | 'system'; accent: string; autoSync: boolean; vault?: Vault; userId?: string };
export class DocFlowDB extends Dexie {
  documents!: Table<DocumentRecord, string>;
  settings!: Table<Settings, string>;
  constructor() {
    super('docflow-v1');
    this.version(1).stores({ documents: 'id, name, modified, folder, deletedAt, favorite', settings: 'id, userId' });
  }
}
export const db = new DocFlowDB();
export const defaultSettings: Settings = { id: 'settings', theme: 'system', accent: '#7168e8', autoSync: true };
