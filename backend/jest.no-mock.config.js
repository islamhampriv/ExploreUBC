const baseConfig = require('./jest.config');

module.exports = {
  ...baseConfig,
  testMatch: [
    '<rootDir>/tests/no-mock/users/**/*.test.ts',
    '<rootDir>/tests/no-mock/itineraries/**/*.test.ts',
  ],
  collectCoverageFrom: [
    'src/users/**/*.ts',
    'src/itineraries/**/*.ts',
    '!src/**/*Types.ts',
    '!src/**/*Repository.ts',
  ],
  coverageThreshold: {
    global: {
      statements: 95,
      branches: 90,
      functions: 95,
      lines: 95,
    },
  },
  coverageDirectory: 'coverage/no-mock',
};
