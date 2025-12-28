#!/usr/bin/env node
// Post-install script to patch @huggingface/transformers v4 for browser compatibility
const fs = require('fs');
const path = require('path');

const packagePath = path.join(__dirname, 'node_modules', '@huggingface', 'transformers', 'package.json');

try {
  if (fs.existsSync(packagePath)) {
    const pkg = JSON.parse(fs.readFileSync(packagePath, 'utf8'));
    
    // Check if it's v4 and needs patching
    if (pkg.version.startsWith('4.0.0')) {
      console.log('Patching @huggingface/transformers v4 for browser compatibility...');
      
      // Update exports to use source files instead of dist files
      pkg.exports = {
        node: {
          import: {
            types: './types/transformers.d.ts',
            default: './src/transformers.js'
          },
          require: {
            types: './types/transformers.d.ts',
            default: './src/transformers.js'
          }
        },
        default: {
          types: './types/transformers.d.ts',
          default: './src/transformers.js'
        }
      };
      
      fs.writeFileSync(packagePath, JSON.stringify(pkg, null, 2));
      console.log('✓ Patched @huggingface/transformers successfully');
    }
  }
} catch (error) {
  console.warn('Warning: Could not patch @huggingface/transformers:', error.message);
}
