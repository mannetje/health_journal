const fs = require('fs'), path = require('path'), { Resvg } = require('@resvg/resvg-js');
const ROOT = path.resolve(__dirname, '../..');
const NL = '\n';
const svg = fs.readFileSync(ROOT + '/design/app-icon/health-journal-logo.svg', 'utf8');
let paths = [...svg.matchAll(/<path fill="(#[0-9a-f]{6})" d="([^"]*)"\/>/g)].map(m => {
  const nums = [...m[2].matchAll(/-?\d+(?:\.\d+)?/g)].map(Number);
  const xs = nums.filter((_, i) => i % 2 == 0), ys = nums.filter((_, i) => i % 2 == 1);
  return { c: m[1], d: m[2].replace(/\s+/g, ' ').trim(), x0: Math.min(...xs), x1: Math.max(...xs), y0: Math.min(...ys), y1: Math.max(...ys) };
});
// drop anti-aliasing specks; the only yellow shape is the pulse line (top of the logo)
paths = paths.filter(p => (p.x1 - p.x0) * (p.y1 - p.y0) > 400 && !(p.c == '#f2ce1b' && p.y0 > 700));
const isHJ = p => p.c == '#c1121f' && p.x0 >= 670 && p.x1 <= 910 && p.y0 >= 970 && p.y1 <= 1170;
console.log(paths.map(p => p.c + ' ' + [p.x0, p.y0, p.x1, p.y1].map(Math.round).join(',') + (isHJ(p) ? ' HJ' : '')).join(NL));

const LIGHT = { '#0560af': '#0560AF', '#c1121f': '#C1121F', '#f2ce1b': '#F2CE1B' };
const DARK = { '#0560af': '#6FB1F2', '#c1121f': '#FF6B6B', '#f2ce1b': '#FFD93D' };
const res = ROOT + '/app/src/main/res';
const w = (f, c) => { fs.mkdirSync(path.dirname(f), { recursive: true }); fs.writeFileSync(f, c); };

// clean master SVG (specks removed)
w(ROOT + '/design/app-icon/health-journal-logo.svg',
  '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 1570 2000">' + NL + '  <title>Health Journal logo</title>' + NL +
  paths.map(p => '  <path fill="' + p.c + '" d="' + p.d + '"/>').join(NL) + NL + '</svg>' + NL);

const P = (p, color, extra = '') =>
  '    <path' + NL + '        android:fillColor="' + color + '"' + extra + NL + '        android:pathData="' + p.d + '" />';

const head = (wdp, hdp, vw, vh) => '<?xml version="1.0" encoding="utf-8"?>' + NL +
  '<vector xmlns:android="http://schemas.android.com/apk/res/android"' + NL +
  '    android:width="' + wdp + 'dp"' + NL + '    android:height="' + hdp + 'dp"' + NL +
  '    android:viewportWidth="' + vw + '"' + NL + '    android:viewportHeight="' + vh + '">' + NL;

// in-app logo, one per theme (viewport = source size)
const logo = m => head(157, 200, 1570, 2000) + paths.map(p => P(p, m[p.c])).join(NL) + NL + '</vector>' + NL;
w(res + '/drawable/logo_health_journal.xml', logo(LIGHT));
w(res + '/drawable-night/logo_health_journal.xml', logo(DARK));
// top app bar logo: the bar is navy in both themes, so this variant is not theme-qualified
w(res + '/drawable/logo_health_journal_on_navy.xml', logo(DARK));

// adaptive foreground / monochrome: 57dp tall logo centred in the 108dp canvas (safe zone is 66dp)
const S = 0.0285, tx = ((108 - 1570 * S) / 2).toFixed(2), ty = ((108 - 2000 * S) / 2).toFixed(2);
const grp = body => head(108, 108, 108, 108) +
  '    <group' + NL + '        android:scaleX="' + S + '"' + NL + '        android:scaleY="' + S + '"' + NL +
  '        android:translateX="' + tx + '"' + NL + '        android:translateY="' + ty + '">' + NL + body + NL + '    </group>' + NL + '</vector>' + NL;
w(res + '/drawable/ic_launcher_foreground.xml', grp(paths.map(p => P(p, DARK[p.c])).join(NL)));
const heavy = NL + '        android:strokeColor="#000000"' + NL + '        android:strokeWidth="9"' + NL + '        android:strokeLineJoin="round"';
w(res + '/drawable/ic_launcher_monochrome.xml', grp(paths.map(p => P(p, '#000000', isHJ(p) ? heavy : '')).join(NL)));
w(res + '/values/ic_launcher_background.xml', '<?xml version="1.0" encoding="utf-8"?>' + NL + '<resources>' + NL + '    <color name="ic_launcher_background">#0B1D3A</color>' + NL + '</resources>' + NL);
const ad = '<?xml version="1.0" encoding="utf-8"?>' + NL +
  '<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">' + NL +
  '    <background android:drawable="@color/ic_launcher_background" />' + NL +
  '    <foreground android:drawable="@drawable/ic_launcher_foreground" />' + NL +
  '    <monochrome android:drawable="@drawable/ic_launcher_monochrome" />' + NL + '</adaptive-icon>' + NL;
w(res + '/mipmap-anydpi-v26/ic_launcher.xml', ad);
w(res + '/mipmap-anydpi-v26/ic_launcher_round.xml', ad);

// raster fallbacks + 512 px icon
const body = paths.map(p => '<path fill="' + DARK[p.c] + '" d="' + p.d + '"/>').join('');
function png(size, shape, frac) {
  const h = size * frac, s = h / 2000, lw = 1570 * s;
  const bg = shape == 'round' ? '<circle cx="' + size / 2 + '" cy="' + size / 2 + '" r="' + size / 2 + '" fill="#0B1D3A"/>'
    : shape == 'square' ? '<rect width="' + size + '" height="' + size + '" rx="' + size * 0.22 + '" fill="#0B1D3A"/>'
    : '<rect width="' + size + '" height="' + size + '" fill="#0B1D3A"/>';
  const x = '<svg xmlns="http://www.w3.org/2000/svg" width="' + size + '" height="' + size + '" viewBox="0 0 ' + size + ' ' + size + '">' + bg +
    '<g transform="translate(' + (size - lw) / 2 + ',' + (size - h) / 2 + ') scale(' + s + ')">' + body + '</g></svg>';
  return new Resvg(x, { fitTo: { mode: 'width', value: size } }).render().asPng();
}
const dens = { mdpi: 48, hdpi: 72, xhdpi: 96, xxhdpi: 144, xxxhdpi: 192 };
for (const [d, sz] of Object.entries(dens)) {
  w(res + '/mipmap-' + d + '/ic_launcher.png', png(sz, 'square', 0.72));
  w(res + '/mipmap-' + d + '/ic_launcher_round.png', png(sz, 'round', 0.64));
}
w(ROOT + '/design/app-icon/health-journal-512.png', png(512, 'full', 0.62));
console.log('done', paths.length);
