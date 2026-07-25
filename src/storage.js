import { openDB } from 'idb';

const DB_NAME = 'house-assessment-db';
const STORE_NAME = 'rooms';

const dbPromise = openDB(DB_NAME, 1, {
  upgrade(db) {
    const store = db.createObjectStore(STORE_NAME, { keyPath: 'id' });
    store.createIndex('updatedAt', 'updatedAt');
  },
});

export async function getAllRooms() {
  const db = await dbPromise;
  return db.getAll(STORE_NAME);
}

export async function saveRoom(room) {
  const db = await dbPromise;
  await db.put(STORE_NAME, room);
  return room;
}

export async function deleteRoom(id) {
  const db = await dbPromise;
  await db.delete(STORE_NAME, id);
}

export async function replaceAllRooms(rooms) {
  const db = await dbPromise;
  const tx = db.transaction(STORE_NAME, 'readwrite');
  await tx.store.clear();
  await Promise.all(rooms.map((room) => tx.store.put(room)));
  await tx.done;
}
