/** @type {import('ts-jest').JestConfigWithTsJest} */
module.exports = {
  preset: 'ts-jest',
  testEnvironment: 'node',
  testMatch: ['**/*.acceptance.test.ts'],
  testTimeout: 30000,
  verbose: true,
  setupFiles: ['./helpers/setup.ts'],
};
