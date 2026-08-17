const cheerio = require('cheerio');

function updateSvgText(svgText, replacements) {
  const $ = cheerio.load(svgText, { xmlMode: true });
  const textElements = $('text').toArray();

  for (let i = 0; i < textElements.length - 2; i++) {
    const t1 = $(textElements[i]).text().trim();
    const t2 = $(textElements[i + 1]).text().trim();
    const t3 = $(textElements[i + 2]).text().trim();

    for (const key in replacements) {
      const newValue = replacements[key];

      // Match: key (e.g., "P"), followed by number, then "mm"
      if (
        t1 === key &&
        /^\d+$/.test(t2) &&
        t3.toLowerCase() === 'mm'
      ) {
        console.log(`✅ Found match: ${t1} ${t2} ${t3} → ${t1} ${newValue} ${t3}`);
        $(textElements[i + 1]).text(`${newValue}`);
      }
    }
  }

  return $.xml();
}

module.exports = updateSvgText;
