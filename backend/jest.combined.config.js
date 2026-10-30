const baseConfig = require('./jest.config');

module.exports = {
  ...baseConfig,
  testMatch: [
    '<rootDir>/tests/no-mock/users/**/*.test.ts',
    '<rootDir>/tests/no-mock/itineraries/**/*.test.ts',
    '<rootDir>/tests/mock/users/**/*.test.ts',
    '<rootDir>/tests/mock/itineraries/**/*.test.ts',
  ],
  collectCoverageFrom: [
    'src/users/**/*.ts',
    'src/itineraries/**/*.ts',
    '!src/**/*Types.ts',
    '!src/**/*Repository.ts',
  ],
  coverageThreshold: {
    global: {
      statements: 98,
      branches: 96,
      functions: 99,
      lines: 98,
    },
  },
  coverageDirectory: 'coverage/combined',
};
