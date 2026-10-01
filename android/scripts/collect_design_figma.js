// use_figma 본문 앞에 const request = { source: fileKey, roots: [...] }; 를 전달한다.
// 캔버스를 수정하지 않는다. 페이지가 다르면 페이지별 호출로 나눈다.
const roots = await Promise.all(request.roots.map(id => figma.getNodeByIdAsync(id)));
if (roots.some(node => !node)) throw new Error('원본 루트가 없습니다');
const pages = roots.map(node => {
  let parent = node;
  while (parent && parent.type !== 'PAGE') parent = parent.parent;
  return parent;
});
if (new Set(pages.map(page => page.id)).size !== 1) throw new Error('페이지별로 수집하십시오');
await figma.setCurrentPageAsync(pages[0]);
figma.skipInvisibleInstanceChildren = false;
const discovered = [];
const graph = [];
function visit(node, parent) {
  if (discovered.includes(node.id)) throw new Error('중첩 또는 중복 루트');
  discovered.push(node.id);
  const children = 'children' in node ? [...node.children] : [];
  graph.push({node, parent, children: children.map(child => child.id)});
  children.forEach(child => visit(child, node.id));
}
roots.forEach(node => visit(node, null));
function serialize(value) {
  if (value === undefined) return {$unavailable: 'undefined'};
  if (typeof value === 'symbol') return {$mixed: true};
  return JSON.parse(JSON.stringify(value, (key, item) => {
    if (typeof item === 'symbol') return {$mixed: true};
    if (item === undefined) return {$unavailable: 'undefined'};
    return item;
  }));
}
const records = graph.map(({node, parent, children}) => {
  const keys = new Set(['id', 'type']);
  for (let proto = node; proto && proto !== Object.prototype; proto = Object.getPrototypeOf(proto)) {
    for (const key of Object.getOwnPropertyNames(proto)) {
      const descriptor = Object.getOwnPropertyDescriptor(proto, key);
      if (descriptor.get) keys.add(key);
    }
  }
  // 관계는 아래 구조 필드에 보존한다. node는 MCP 내부 프록시이다.
  ['parent', 'children', 'node'].forEach(key => keys.delete(key));
  const properties = {};
  const unavailable = {};
  for (const key of [...keys].sort()) {
    try { properties[key] = serialize(node[key]); }
    catch (error) { unavailable[key] = String(error); }
  }
  return {id: node.id, parent, children, declaredProperties: [...keys].sort(), properties, unavailable};
});
const snapshot = {schema: 1, adapter: 'figma-runtime-properties-v1', source: request.source,
  roots: request.roots, discovered, records,
  limitations: ['MCP 노출 getter만 수집. 미지원 속성은 원본 보충 전 완료 불가.',
    '외부 컴포넌트, 변수 정의, 이미지 바이트 및 미제공 상태는 별도 원본과 검증 필요.']};
// MCP 응답 길이 제한을 넘으면 같은 원본을 offset별로 받아 합친다.
// 반복 속성을 LZW16으로 압축한다. 원본 내용 자체를 생략하지 않는다.
const utf8 = unescape(encodeURIComponent(JSON.stringify(snapshot)));
const dictionary = new Map();
let nextCode = 256;
let word = '';
const codes = [];
const codeOf = text => text.length === 1 ? text.charCodeAt(0) : dictionary.get(text);
for (const character of utf8) {
  const joined = word + character;
  if (joined.length === 1 || dictionary.has(joined)) word = joined;
  else {
    codes.push(codeOf(word));
    if (nextCode < 65536) dictionary.set(joined, nextCode++);
    word = character;
  }
}
if (word) codes.push(codeOf(word));
const bytes = codes.flatMap(code => [code >> 8, code & 255]);
const alphabet = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/';
let encoded = '';
for (let i = 0; i < bytes.length; i += 3) {
  const value = (bytes[i] << 16) | ((bytes[i + 1] || 0) << 8) | (bytes[i + 2] || 0);
  encoded += alphabet[(value >> 18) & 63] + alphabet[(value >> 12) & 63]
    + (i + 1 < bytes.length ? alphabet[(value >> 6) & 63] : '=')
    + (i + 2 < bytes.length ? alphabet[value & 63] : '=');
}
const offset = request.offset || 0;
let checksum = 2166136261;
for (let i = 0; i < encoded.length; i++) checksum = Math.imul(checksum ^ encoded.charCodeAt(i), 16777619) >>> 0;
return {encoding: 'lzw16-base64', total: encoded.length, offset,
  chunk: encoded.slice(offset, offset + 16000), nodes: records.length, checksum};
