const fs = require('fs');
const path = require('path');

// Missing iLib data lets compilation succeed but crashes I18nDecorator on the TV.
const output = path.join(__dirname, '../dist');
for (const file of ['index.html', 'main.js', 'node_modules/ilib/locale/ilibmanifest.json',
  'node_modules/ilib/locale/localeinfo.json']) {
  if (!fs.existsSync(path.join(output, file))) {
    throw new Error(`Missing runtime asset: ${file}. Build with iLib asset emission enabled before packaging.`);
  }
}
const manifest = JSON.parse(fs.readFileSync(path.join(output, 'node_modules/ilib/locale/ilibmanifest.json'), 'utf8'));
if (!Array.isArray(manifest.files) || manifest.files.length === 0) {
  throw new Error('The iLib locale manifest must contain its runtime file list.');
}
console.log('TV runtime assets verified.');
