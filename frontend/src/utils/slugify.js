// Mirrors the backend's Slugs.of: cosmetic only, routing uses the id.
export function slugify(title) {
  if (!title) return '';
  const s = title
    .toLowerCase()
    .replace(/ä/g, 'a').replace(/ö/g, 'o').replace(/ü/g, 'u').replace(/ß/g, 'ss')
    .normalize('NFD').replace(/[̀-ͯ]/g, '')
    .replace(/[^a-z0-9]+/g, '-')
    .replace(/^-|-$/g, '');
  return s.length > 60 ? s.slice(0, 60).replace(/-$/, '') : s;
}
