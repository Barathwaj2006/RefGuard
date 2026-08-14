const fs = require('fs');
const path = require('path');

let totalTests = 0;
let passedTests = 0;
let failedTests = 0;

global.describe = (suiteName, fn) => {
  console.log('\n📦 ' + suiteName);
  fn();
};

let currentBeforeEach = null;
global.beforeEach = (fn) => {
  currentBeforeEach = fn;
};

global.it = (testName, fn) => {
  totalTests++;
  try {
    if (currentBeforeEach) currentBeforeEach();
    fn();
    console.log('  ✅ PASS: ' + testName);
    passedTests++;
  } catch (err) {
    console.error('  ❌ FAIL: ' + testName);
    console.error('     ' + err.message);
    if (err.stack) {
      console.error(err.stack.split('\n').slice(1, 4).join('\n'));
    }
    failedTests++;
  }
};

console.log('====================================================');
console.log('🛡️  RefGuard Full-System Integration & E2E Test Suite');
console.log('====================================================');

require('./contract.test.js');
require('./e2e.test.js');

console.log('\n====================================================');
console.log('Test Summary: ' + passedTests + '/' + totalTests + ' passed (' + failedTests + ' failed)');
console.log('====================================================');

if (failedTests > 0) {
  process.exit(1);
} else {
  console.log('🎉 ALL INTEGRATION & CONTRACT TESTS PASSED!');
  process.exit(0);
}
