const baseConfig = require('./jest.config');

module.exports = {
  ...baseConfig,
  testMatch: [
    '<rootDir>/tests/mock/users/**/*.test.ts',
    '<rootDir>/tests/mock/itineraries/**/*.test.ts',
  ],
  collectCoverageFrom: [
    'src/users/**/*.ts',
    'src/itineraries/**/*.ts',
    '!src/**/*Types.ts',
    '!src/**/*Repository.ts',
  ],
  coverageDirectory: 'coverage/mock',
};
