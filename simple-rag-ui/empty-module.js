// Empty module stub for Node.js modules that don't exist in browser
// Export common stream/fs/path APIs as no-ops
export default {};
export const Readable = class {};
export const Writable = class {};
export const Transform = class {};
export const pipeline = () => Promise.resolve();
export const createReadStream = () => new Readable();
export const createWriteStream = () => new Writable();
export const readFile = () => Promise.resolve('');
export const writeFile = () => Promise.resolve();
export const mkdir = () => Promise.resolve();
export const stat = () => Promise.resolve({});
export const readdir = () => Promise.resolve([]);
export const join = (...args) => args.join('/');
export const resolve = (...args) => args.join('/');
export const dirname = (p) => p;
export const basename = (p) => p;
export const extname = (p) => '';
export const parse = (p) => ({ root: '', dir: '', base: '', ext: '', name: '' });

