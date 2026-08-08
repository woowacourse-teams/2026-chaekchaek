import fs from 'fs';
import path from 'path';

const [name] = process.argv.slice(2);
if (!name) throw new Error('You must include a component name');

const dir = path.resolve('./src/components', `./${name}/`);
if (fs.existsSync(dir)) throw new Error('A component with that name already exists');

fs.mkdirSync(dir);

const componentDir = path.resolve('./.generate/component', './Example');

fs.readdir(componentDir, (err, filelist) => {
  if (err) throw err;
  filelist.forEach(async (file) => {
    let filename = file.replace('.template', '').replace('Example', name);

    fs.readFile(path.resolve(componentDir, file), 'utf8', (readErr, raw) => {
      if (readErr) throw readErr;

      const componentName = name;

      const replacements = {
        __COMPONENT_NAME__: componentName,
        __COMPONENT_NAME_LOWER__: componentName.toLowerCase(),
      };

      let content = raw;
      for (const [key, value] of Object.entries(replacements)) {
        content = content.replaceAll(key, value);
      }

      fs.writeFile(`${dir}/${filename}`, content, (err) => {
        if (err) throw err;
      });
    });
  });
});
