import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { auth, offers, users } from './client';
import { setStoredUser } from '../auth/authStorage';

function jsonResponse(body, { status = 200, statusText = 'OK' } = {}) {
  return {
    ok: status >= 200 && status < 300,
    status,
    statusText,
    json: () => (body === undefined ? Promise.reject(new Error('no body')) : Promise.resolve(body)),
  };
}

describe('api/client request handling', () => {
  beforeEach(() => {
    localStorage.clear();
    vi.stubGlobal('fetch', vi.fn());
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it('resolves with the parsed JSON body on success', async () => {
    const body = [{ id: '1', title: 'Free sofa' }];
    fetch.mockResolvedValue(jsonResponse(body));

    await expect(offers.list()).resolves.toEqual(body);
    expect(fetch).toHaveBeenCalledWith('/api/offers', expect.objectContaining({
      headers: expect.objectContaining({ 'Content-Type': 'application/json' }),
    }));
  });

  it('throws the joined defaultMessage list from a Spring validation error body', async () => {
    fetch.mockResolvedValue(jsonResponse(
      { errors: [{ defaultMessage: 'title must not be blank' }, { defaultMessage: 'quantity must be positive' }] },
      { status: 400, statusText: 'Bad Request' },
    ));

    await expect(offers.create({})).rejects.toThrow(
      'title must not be blank, quantity must be positive',
    );
  });

  it('throws the message from a plain { error } body', async () => {
    fetch.mockResolvedValue(jsonResponse(
      { error: 'Username already taken' },
      { status: 409, statusText: 'Conflict' },
    ));

    await expect(users.create({ username: 'dup' })).rejects.toThrow('Username already taken');
  });

  it('falls back to "status statusText" when the error body is not JSON', async () => {
    fetch.mockResolvedValue(jsonResponse(undefined, { status: 500, statusText: 'Internal Server Error' }));

    await expect(offers.list()).rejects.toThrow('500 Internal Server Error');
  });

  it('attaches X-Session-Token from the stored user, and omits it when signed out', async () => {
    fetch.mockResolvedValue(jsonResponse({}));

    await offers.list();
    expect(fetch).toHaveBeenLastCalledWith('/api/offers', expect.objectContaining({
      headers: expect.not.objectContaining({ 'X-Session-Token': expect.anything() }),
    }));

    setStoredUser({ id: 'u1', username: 'tim', token: 'tok-123' });
    await offers.list();
    expect(fetch).toHaveBeenLastCalledWith('/api/offers', expect.objectContaining({
      headers: expect.objectContaining({ 'X-Session-Token': 'tok-123' }),
    }));
  });

  // Note: client.js also sets window.location.href = '/login' on 401; jsdom cannot
  // navigate, so vitest prints a harmless "Not implemented: navigation" line here.
  it('clears the stored user and throws "Session expired." on a 401', async () => {
    setStoredUser({ id: 'u1', username: 'tim', token: 'stale-token' });
    fetch.mockResolvedValue(jsonResponse({ error: 'unauthorized' }, { status: 401, statusText: 'Unauthorized' }));
    const removeSpy = vi.spyOn(Storage.prototype, 'removeItem');

    await expect(auth.logout()).rejects.toThrow('Session expired.');
    expect(removeSpy).toHaveBeenCalledWith('currentUser');
    expect(localStorage.getItem('currentUser')).toBeNull();
    removeSpy.mockRestore();
  });
});
