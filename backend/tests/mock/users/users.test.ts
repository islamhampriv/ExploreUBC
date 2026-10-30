import request from 'supertest';

import { createApp } from '../../../src/index';
import type { UserRepository } from '../../../src/users/userRepository';

const USER_ID = '6705f0000000000000000001';

function createMockRepository(): jest.Mocked<UserRepository> {
  return {
    findPreferences: jest.fn(),
    findUser: jest.fn(),
    updateUser: jest.fn(),
  };
}

describe('GET /api/v1/users/:userId — mocks', () => {
  test('returns 500 when the database read fails', async () => {
    // Input: valid user ID. Expect: 500 INTERNAL_SERVER_ERROR.
    // Mock: findUser rejects to simulate an unavailable database.
    const repository = createMockRepository();
    repository.findUser.mockRejectedValue(new Error('database unavailable'));
    const app = createApp({ userRepository: repository });

    const response = await request(app).get(`/api/v1/users/${USER_ID}`);

    expect(response.status).toBe(500);
    expect(response.body.error).toEqual({
      code: 'INTERNAL_SERVER_ERROR',
      message: 'An unexpected error occurred.',
    });
    expect(repository.findUser).toHaveBeenCalledWith(USER_ID);
  });
});

describe('PATCH /api/v1/users/:userId — mocks', () => {
  test('returns 500 when the database update fails', async () => {
    // Input: valid profile update. Expect: 500 INTERNAL_SERVER_ERROR.
    // Mock: updateUser rejects to simulate a database write failure.
    const repository = createMockRepository();
    repository.updateUser.mockRejectedValue(new Error('database unavailable'));
    const app = createApp({ userRepository: repository });

    const response = await request(app)
      .patch(`/api/v1/users/${USER_ID}`)
      .send({ displayName: 'Updated User' });

    expect(response.status).toBe(500);
    expect(response.body.error.code).toBe('INTERNAL_SERVER_ERROR');
    expect(repository.updateUser).toHaveBeenCalledWith(USER_ID, {
      displayName: 'Updated User',
    });
  });

  test('returns 500 when reading the user back fails', async () => {
    // Input: valid profile update. Expect: 500 after the successful write.
    // Mock: updateUser resolves true, then findUser rejects during response loading.
    const repository = createMockRepository();
    repository.updateUser.mockResolvedValue(true);
    repository.findUser.mockRejectedValue(new Error('database unavailable'));
    const app = createApp({ userRepository: repository });

    const response = await request(app)
      .patch(`/api/v1/users/${USER_ID}`)
      .send({ start_location: 'UBC Nest' });

    expect(response.status).toBe(500);
    expect(response.body.error.code).toBe('INTERNAL_SERVER_ERROR');
    expect(repository.updateUser).toHaveBeenCalledTimes(1);
    expect(repository.findUser).toHaveBeenCalledWith(USER_ID);
  });
});
